package ru.mirea.parking.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.parking.dto.csv.ImportApplyRequest;
import ru.mirea.parking.dto.csv.ImportPreview;
import ru.mirea.parking.dto.csv.ImportPreview.RowError;
import ru.mirea.parking.dto.csv.ImportResult;
import ru.mirea.parking.error.BadRequestException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Общий помощник импорта и экспорта CSV: сущность описывает только заголовки, разбор строки и запись. */
@Component
public class CsvSupport {

    public static final char DELIMITER = ';';
    public static final int MAX_FILE_BYTES = 2 * 1024 * 1024;
    public static final int MAX_ROWS = 5000;
    public static final MediaType TEXT_CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final String DANGEROUS_START = "=+-@\t\r";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Charset WINDOWS_1251 = Charset.forName("windows-1251");

    private final ImportTokenStore tokens;
    private final TransactionTemplate transaction;

    public CsvSupport(ImportTokenStore tokens, PlatformTransactionManager transactionManager) {
        this.tokens = tokens;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /** Результат записи одной строки импорта. */
    public enum Outcome { CREATED, UPDATED, SKIPPED }

    /** Запись одной разобранной строки в базу с учётом режима импорта. */
    @FunctionalInterface
    public interface RowWriter<T> {
        Outcome write(T value, ImportMode mode);
    }

    /** CSV для Excel: UTF-8 с BOM, разделитель «;», защита от формул в ячейках. */
    public byte[] write(List<String> headers, List<List<?>> rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(UTF8_BOM);
        CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(DELIMITER).setRecordSeparator("\r\n").get();
        try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, format)) {
            printer.printRecord(headers);
            for (List<?> row : rows) {
                printer.printRecord(row.stream().map(CsvSupport::format).toList());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сформировать CSV", e);
        }
        return out.toByteArray();
    }

    /** Ответ со скачиванием файла. */
    public ResponseEntity<byte[]> attachment(String filename, byte[] body) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .contentType(TEXT_CSV)
                .body(body);
    }

    /** Значение ячейки: числа с запятой как в русском Excel, даты без секунд, булевы да/нет, формулы обезврежены. */
    static String format(Object value) {
        String text = switch (value) {
            case null -> "";
            case BigDecimal decimal -> decimal.toPlainString().replace('.', ',');
            case Boolean bool -> bool ? "да" : "нет";
            case LocalDateTime dateTime -> dateTime.format(DATE_TIME);
            case LocalDate date -> date.toString();
            default -> value.toString();
        };
        return escapeFormula(text);
    }

    /** Защита от CSV-инъекций: ячейка, начинающаяся с = + - @, получает апостроф и не выполняется как формула. */
    static String escapeFormula(String text) {
        return !text.isEmpty() && DANGEROUS_START.indexOf(text.charAt(0)) >= 0 ? "'" + text : text;
    }

    static String unescapeFormula(String text) {
        return text.length() > 1 && text.charAt(0) == '\'' && DANGEROUS_START.indexOf(text.charAt(1)) >= 0
                ? text.substring(1) : text;
    }

    /** Читает файл, проверяет размер, кодировку, заголовки и число строк. */
    public List<CsvRow> read(byte[] content, List<String> requiredHeaders) {
        if (content.length == 0) {
            throw new BadRequestException("file", "Файл пустой");
        }
        if (content.length > MAX_FILE_BYTES) {
            throw new BadRequestException("file", "Файл больше " + MAX_FILE_BYTES / 1024 / 1024 + " МБ");
        }
        String text = decode(content);
        String firstLine = text.lines().findFirst().orElse("");
        char delimiter = firstLine.indexOf(';') >= 0 ? ';' : ',';
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setIgnoreSurroundingSpaces(true)
                .setAllowMissingColumnNames(true)
                .get();
        try (CSVParser parser = CSVParser.builder().setReader(new StringReader(text)).setFormat(format).get()) {
            Map<String, Integer> headerIndex = new HashMap<>();
            parser.getHeaderNames().forEach(h -> headerIndex.put(h.trim().toLowerCase(Locale.ROOT), headerIndex.size()));
            checkHeaders(headerIndex.keySet(), requiredHeaders);

            List<CsvRow> rows = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (rows.size() >= MAX_ROWS) {
                    throw new BadRequestException("file", "В файле больше " + MAX_ROWS + " строк");
                }
                Map<String, String> values = new LinkedHashMap<>();
                headerIndex.forEach((header, index) ->
                        values.put(header, index < record.size() ? unescapeFormula(record.get(index)) : ""));
                rows.add(new CsvRow((int) record.getRecordNumber() + 1, values));
            }
            return rows;
        } catch (IOException | IllegalStateException | IllegalArgumentException e) {
            throw new BadRequestException("file", "Не удалось прочитать CSV: проверьте кавычки и разделитель «;»");
        }
    }

    private static void checkHeaders(Set<String> actual, List<String> required) {
        List<String> missing = required.stream().filter(h -> !actual.contains(h.toLowerCase(Locale.ROOT))).toList();
        if (!missing.isEmpty()) {
            throw new BadRequestException("file", "В файле нет столбцов: " + String.join(", ", missing)
                    + ". Ожидаются: " + String.join(";", required));
        }
    }

    /** UTF-8 (с BOM или без); если файл сохранён Excel в Windows-1251, читаем его как Windows-1251. */
    static String decode(byte[] content) {
        int offset = startsWithBom(content) ? UTF8_BOM.length : 0;
        ByteBuffer buffer = ByteBuffer.wrap(content, offset, content.length - offset);
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(buffer).toString();
        } catch (CharacterCodingException e) {
            return new String(content, offset, content.length - offset, WINDOWS_1251);
        }
    }

    private static boolean startsWithBom(byte[] content) {
        return content.length >= 3 && content[0] == UTF8_BOM[0] && content[1] == UTF8_BOM[1] && content[2] == UTF8_BOM[2];
    }

    /** Проверка файла без записи в базу; строки разбираются тем же parser, что и при apply. */
    public <T> ImportPreview preview(String entity, MultipartFile file, List<String> requiredHeaders,
                                     Function<CsvRow, T> parser, Function<T, String> key) {
        byte[] content = bytes(file);
        List<CsvRow> rows = read(content, requiredHeaders);
        List<RowError> errors = new ArrayList<>();
        int valid = 0;
        Set<String> seenKeys = new HashSet<>();
        for (CsvRow row : rows) {
            String error = transaction.execute(status -> {
                status.setRollbackOnly();
                return parseError(row, parser, key, seenKeys);
            });
            if (error == null) {
                valid++;
            } else {
                errors.add(new RowError(row.line(), error));
            }
        }
        ImportTokenStore.Stored stored = tokens.put(entity, currentUser(), content);
        return new ImportPreview(rows.size(), valid, errors.size(), errors, stored.token(), stored.expiresAt());
    }

    /** Применение файла по importToken: каждая строка в своей транзакции, ошибка строки не отменяет остальные. */
    public <T> ImportResult apply(String entity, ImportApplyRequest request, List<String> requiredHeaders,
                                  Function<CsvRow, T> parser, Function<T, String> key, RowWriter<T> writer) {
        byte[] content = tokens.take(request.importToken(), entity, currentUser());
        List<CsvRow> rows = read(content, requiredHeaders);
        List<RowError> errors = new ArrayList<>();
        int created = 0;
        int updated = 0;
        int skipped = 0;
        Set<String> seenKeys = new HashSet<>();
        for (CsvRow row : rows) {
            try {
                Outcome outcome = transaction.execute(status -> {
                    T value = parser.apply(row);
                    checkDuplicate(key.apply(value), seenKeys);
                    return writer.write(value, request.mode());
                });
                switch (outcome) {
                    case CREATED -> created++;
                    case UPDATED -> updated++;
                    case SKIPPED -> skipped++;
                }
            } catch (RuntimeException e) {
                errors.add(new RowError(row.line(), messageOf(e)));
            }
        }
        return new ImportResult(request.mode(), created, updated, skipped, errors);
    }

    private static <T> String parseError(CsvRow row, Function<CsvRow, T> parser, Function<T, String> key, Set<String> seenKeys) {
        try {
            T value = parser.apply(row);
            checkDuplicate(key.apply(value), seenKeys);
            return null;
        } catch (RuntimeException e) {
            return messageOf(e);
        }
    }

    private static void checkDuplicate(String key, Set<String> seenKeys) {
        if (!seenKeys.add(key.toLowerCase(Locale.ROOT))) {
            throw new CsvRowException("Запись «" + key + "» уже встречалась в файле выше");
        }
    }

    private static String messageOf(RuntimeException e) {
        if (e instanceof CsvRowException) {
            return e.getMessage();
        }
        if (e instanceof ResponseStatusException status && status.getReason() != null) {
            return status.getReason();
        }
        if (e instanceof org.springframework.dao.DataIntegrityViolationException) {
            return "Запись конфликтует с уже существующими данными";
        }
        return "Строку не удалось обработать";
    }

    private static byte[] bytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("file", "Файл не выбран или пустой");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BadRequestException("file", "Файл больше " + MAX_FILE_BYTES / 1024 / 1024 + " МБ");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("file", "Не удалось прочитать файл");
        }
    }

    private static String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "anonymous" : auth.getName();
    }
}

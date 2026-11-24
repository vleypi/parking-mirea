package ru.mirea.parking.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.parking.csv.CsvRow;
import ru.mirea.parking.csv.CsvRowException;
import ru.mirea.parking.csv.CsvSupport;
import ru.mirea.parking.csv.CsvSupport.Outcome;
import ru.mirea.parking.csv.ImportMode;
import ru.mirea.parking.domain.Spot;
import ru.mirea.parking.domain.SpotStatus;
import ru.mirea.parking.domain.SpotType;
import ru.mirea.parking.dto.csv.ImportApplyRequest;
import ru.mirea.parking.dto.csv.ImportPreview;
import ru.mirea.parking.dto.csv.ImportResult;
import ru.mirea.parking.dto.spot.SpotCreateRequest;
import ru.mirea.parking.dto.spot.SpotResponse;
import ru.mirea.parking.dto.spot.SpotUpdateRequest;
import ru.mirea.parking.filter.SpotFilter;
import ru.mirea.parking.repository.SpotRepository;
import ru.mirea.parking.repository.SpotTypeRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Пример подключения CsvSupport: экспорт и импорт мест; ключ записи — номер места, тип задаётся кодом. */
@Service
public class SpotCsvService {

    public static final String ENTITY = "spots";
    public static final String NUMBER = "номер";
    public static final String TYPE = "тип";
    public static final String RATE = "тариф";
    public static final String ZONE = "зона";
    public static final String DESCRIPTION = "описание";
    public static final String STATUS = "статус";
    public static final String COVERED = "крытое";
    public static final String TAGS = "теги";

    /** Обязательные столбцы импорта. */
    public static final List<String> REQUIRED = List.of(NUMBER, TYPE, RATE);
    /** Столбцы экспорта: те же, что читает импорт, плюс справочные (название типа, доступность, архив). */
    public static final List<String> EXPORT = List.of(NUMBER, TYPE, "тип_название", RATE, ZONE, DESCRIPTION, STATUS,
            COVERED, "доступность", TAGS, "архив");

    private static final BigDecimal MAX_RATE = new BigDecimal("99999.99");

    private final CsvSupport csv;
    private final SpotService spotService;
    private final SpotRepository spots;
    private final SpotTypeRepository spotTypes;
    private final Clock clock;

    public SpotCsvService(CsvSupport csv, SpotService spotService, SpotRepository spots, SpotTypeRepository spotTypes,
                          Clock clock) {
        this.csv = csv;
        this.spotService = spotService;
        this.spots = spots;
        this.spotTypes = spotTypes;
        this.clock = clock;
    }

    /** Разобранная строка файла. */
    record Row(int number, SpotType type, BigDecimal rate, String zone, String description, SpotStatus status,
               boolean covered, Set<String> tags) {
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> export(SpotFilter filter) {
        List<List<?>> rows = spotService.searchAll(filter).stream().map(SpotCsvService::toCsv).toList();
        return csv.attachment("spots-" + LocalDate.now(clock) + ".csv", csv.write(EXPORT, rows));
    }

    public ImportPreview preview(MultipartFile file) {
        return csv.preview(ENTITY, file, REQUIRED, this::parse, row -> String.valueOf(row.number()));
    }

    public ImportResult apply(ImportApplyRequest request) {
        return csv.apply(ENTITY, request, REQUIRED, this::parse, row -> String.valueOf(row.number()), this::write);
    }

    private static List<?> toCsv(SpotResponse s) {
        return Arrays.asList(s.spotNumber(), s.spotType().code(), s.spotType().title(), s.hourlyRate(), s.zone(),
                s.description(), s.status().code(), s.covered(), s.availability().title(), String.join(",", s.tags()),
                s.archived());
    }

    /** Разбор строки: те же проверки, что у формы (номер, активный тип, тариф, длины). */
    Row parse(CsvRow row) {
        int number = row.requiredInt(NUMBER);
        if (number <= 0) {
            throw new CsvRowException("Номер места должен быть положительным, получено " + number);
        }
        SpotType type = findType(row.required(TYPE));
        BigDecimal rate = row.requiredDecimal(RATE);
        if (rate.signum() < 0 || rate.compareTo(MAX_RATE) > 0 || rate.scale() > 2) {
            throw new CsvRowException(SpotCreateRequest.RATE_MESSAGE + ", получено " + rate.toPlainString());
        }
        String zone = row.optional(ZONE);
        if (zone != null && zone.length() > 20) {
            throw new CsvRowException("Зона не длиннее 20 символов");
        }
        String description = row.optional(DESCRIPTION);
        if (description != null && description.length() > 500) {
            throw new CsvRowException("Описание не длиннее 500 символов");
        }
        SpotStatus status = row.optional(STATUS) == null ? SpotStatus.AVAILABLE
                : row.enumValue(STATUS, SpotStatus.class, SpotStatus::getTitle);
        Set<String> tags = Arrays.stream(row.get(TAGS).split("[,;]"))
                .map(SpotService::normalizeTag).filter(t -> !t.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new Row(number, type, rate, zone, description, status, row.bool(COVERED), tags);
    }

    /** CREATE_ONLY пропускает существующие номера, UPSERT обновляет их. */
    private Outcome write(Row row, ImportMode mode) {
        Optional<Spot> existing = spots.findBySpotNumber(row.number());
        if (existing.isEmpty()) {
            spotService.create(new SpotCreateRequest(row.number(), row.type().getId(), row.rate(), row.zone(),
                    row.description(), row.status(), row.covered(), row.tags()));
            return Outcome.CREATED;
        }
        if (mode == ImportMode.CREATE_ONLY) {
            return Outcome.SKIPPED;
        }
        Spot spot = existing.get();
        spotService.update(spot.getUuid(), new SpotUpdateRequest(row.number(), row.type().getId(), row.rate(), row.zone(),
                row.description(), row.status(), row.covered(), row.tags(), spot.getVersion()));
        return Outcome.UPDATED;
    }

    /** Тип по коду STANDARD, DISABLED, ELECTRIC; неизвестный или отключённый — ошибка строки. */
    private SpotType findType(String code) {
        SpotType type = spotTypes.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new CsvRowException("Неизвестный тип места «" + code + "»"));
        if (!type.isActive()) {
            throw new CsvRowException("Тип места «" + type.getTitle() + "» отключён");
        }
        return type;
    }
}

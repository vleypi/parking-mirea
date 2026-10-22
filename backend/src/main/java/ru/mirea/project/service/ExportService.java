package ru.mirea.project.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.enums.LookupValue;

public class ExportService {
    private static final String DEFAULT_SHEET_NAME = "Данные";
    private static final char CSV_DELIMITER = ';';
    private static final String CSV_LINE_BREAK = "\r\n";
    private static final char CSV_BOM = '\uFEFF';
    private static final DateTimeFormatter TEXT_DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter TEXT_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String XLSX_DATE_TIME_FORMAT = "dd.mm.yyyy hh:mm";
    private static final String XLSX_DATE_FORMAT = "dd.mm.yyyy";
    private static final int MIN_COLUMN_WIDTH = 8;
    private static final int MAX_COLUMN_WIDTH = 60;

    static {
        // POI логирует через log4j-api без реализации; встроенный провайдер убирает ERROR в консоли
        System.setProperty("log4j2.loggerContextFactory", "org.apache.logging.log4j.simple.SimpleLoggerContextFactory");
    }

    private enum Format {
        XLSX, CSV
    }

    public void export(List<String> headers, List<? extends List<?>> rows, Path file) {
        export(DEFAULT_SHEET_NAME, headers, rows, file);
    }

    public void export(String sheetName, List<String> headers, List<? extends List<?>> rows, Path file) {
        Format format = detectFormat(file);
        checkData(headers, rows);
        try {
            switch (format) {
                case XLSX -> writeXlsx(sheetName, headers, rows, file);
                case CSV -> writeCsv(headers, rows, file);
            }
        } catch (IOException e) {
            throw new BusinessException("Не удалось сохранить файл «" + file.getFileName()
                + "». Проверьте, что он не открыт в другой программе и что в эту папку можно записывать");
        }
    }

    private Format detectFormat(Path file) {
        if (file == null || file.getFileName() == null) {
            throw new BusinessException("Не выбран файл для экспорта");
        }
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".xlsx")) {
            return Format.XLSX;
        }
        if (name.endsWith(".csv")) {
            return Format.CSV;
        }
        throw new BusinessException("Неподдерживаемый формат файла. Сохраните таблицу как .xlsx или .csv");
    }

    private void checkData(List<String> headers, List<? extends List<?>> rows) {
        if (headers == null || headers.isEmpty()) {
            throw new BusinessException("Нет столбцов для экспорта");
        }
        if (rows == null) {
            throw new IllegalArgumentException("Список строк для экспорта не задан");
        }
        for (int i = 0; i < rows.size(); i++) {
            List<?> row = rows.get(i);
            if (row == null || row.size() != headers.size()) {
                throw new IllegalArgumentException("Строка " + (i + 1) + " содержит "
                    + (row == null ? 0 : row.size()) + " значений, а столбцов " + headers.size());
            }
        }
    }

    private void writeXlsx(String sheetName, List<String> headers, List<? extends List<?>> rows, Path file)
        throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            String safeName = WorkbookUtil.createSafeSheetName(
                sheetName == null || sheetName.isBlank() ? DEFAULT_SHEET_NAME : sheetName);
            Sheet sheet = workbook.createSheet(safeName);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateTimeStyle = createDateStyle(workbook, XLSX_DATE_TIME_FORMAT);
            CellStyle dateStyle = createDateStyle(workbook, XLSX_DATE_FORMAT);

            Row headerRow = sheet.createRow(0);
            for (int column = 0; column < headers.size(); column++) {
                Cell cell = headerRow.createCell(column);
                cell.setCellValue(headers.get(column));
                cell.setCellStyle(headerStyle);
            }
            sheet.createFreezePane(0, 1);

            for (int i = 0; i < rows.size(); i++) {
                Row row = sheet.createRow(i + 1);
                List<?> values = rows.get(i);
                for (int column = 0; column < values.size(); column++) {
                    setCell(row.createCell(column), values.get(column), dateTimeStyle, dateStyle);
                }
            }

            for (int column = 0; column < headers.size(); column++) {
                sheet.setColumnWidth(column, columnWidth(headers, rows, column) * 256);
            }

            try (OutputStream out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        }
    }

    private void setCell(Cell cell, Object value, CellStyle dateTimeStyle, CellStyle dateStyle) {
        switch (value) {
            case null -> {
            }
            case LocalDateTime dateTime -> setDate(cell, dateTime, dateTimeStyle);
            case LocalDate date -> setDate(cell, date.atStartOfDay(), dateStyle);
            case Boolean flag -> cell.setCellValue(flag);
            case LookupValue lookup -> cell.setCellValue(lookup.getTitle());
            case Number number -> cell.setCellValue(number.doubleValue());
            default -> cell.setCellValue(value.toString());
        }
    }

    private void setDate(Cell cell, LocalDateTime value, CellStyle style) {
        cell.setCellValue(Date.from(value.atZone(ZoneId.systemDefault()).toInstant()));
        cell.setCellStyle(style);
    }

    private int columnWidth(List<String> headers, List<? extends List<?>> rows, int column) {
        int width = headers.get(column).length();
        for (List<?> row : rows) {
            width = Math.max(width, formatText(row.get(column)).length());
        }
        return Math.min(Math.max(width + 2, MIN_COLUMN_WIDTH), MAX_COLUMN_WIDTH);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook, String format) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat(format));
        return style;
    }

    private void writeCsv(List<String> headers, List<? extends List<?>> rows, Path file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(CSV_BOM);
            writeCsvLine(writer, headers);
            for (List<?> row : rows) {
                writeCsvLine(writer, row.stream().map(this::formatText).toList());
            }
        }
    }

    private void writeCsvLine(BufferedWriter writer, List<String> values) throws IOException {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                line.append(CSV_DELIMITER);
            }
            line.append(escapeCsv(values.get(i)));
        }
        line.append(CSV_LINE_BREAK);
        writer.write(line.toString());
    }

    private String escapeCsv(String text) {
        boolean needsQuotes = text.indexOf(CSV_DELIMITER) >= 0 || text.indexOf('"') >= 0
            || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0;
        return needsQuotes ? '"' + text.replace("\"", "\"\"") + '"' : text;
    }

    private String formatText(Object value) {
        return switch (value) {
            case null -> "";
            case LocalDateTime dateTime -> dateTime.format(TEXT_DATE_TIME);
            case LocalDate date -> date.format(TEXT_DATE);
            case BigDecimal number -> number.toPlainString();
            case LookupValue lookup -> lookup.getTitle();
            default -> value.toString();
        };
    }
}

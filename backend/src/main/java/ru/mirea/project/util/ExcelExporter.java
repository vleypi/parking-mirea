package ru.mirea.project.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import ru.mirea.project.model.ParkingRequest;
import ru.mirea.project.model.User;

public final class ExcelExporter {
    private static final String[] USER_HEADERS = {"ID", "Имя", "Телефон", "Создан"};
    private static final int[] USER_WIDTHS = {8, 30, 20, 20};
    private static final String[] REQUEST_HEADERS =
        {"ID", "ID владельца", "Гос. номер", "Место", "Начало", "Окончание", "Статус", "Создана"};
    private static final int[] REQUEST_WIDTHS = {8, 14, 16, 8, 20, 20, 14, 20};
    private static final String DATE_FORMAT = "dd.mm.yyyy hh:mm";

    static {
        // POI логирует через log4j-api без реализации; встроенный провайдер убирает ERROR в консоли
        System.setProperty("log4j2.loggerContextFactory", "org.apache.logging.log4j.simple.SimpleLoggerContextFactory");
    }

    private ExcelExporter() {
    }

    public static void export(List<User> users, List<ParkingRequest> requests, Path file) {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            writeUsers(workbook.createSheet("Владельцы"), users, headerStyle, dateStyle);
            writeRequests(workbook.createSheet("Заявки"), requests, headerStyle, dateStyle);

            try (OutputStream out = Files.newOutputStream(file)) {
                workbook.write(out);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось сохранить файл " + file, e);
        }
    }

    private static void writeUsers(Sheet sheet, List<User> users, CellStyle headerStyle, CellStyle dateStyle) {
        writeHeader(sheet, USER_HEADERS, USER_WIDTHS, headerStyle);

        int rowIndex = 1;
        for (User user : users) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(user.getId());
            row.createCell(1).setCellValue(user.getName());
            row.createCell(2).setCellValue(user.getPhone());
            setDate(row.createCell(3), user.getCreatedAt(), dateStyle);
        }
    }

    private static void writeRequests(Sheet sheet, List<ParkingRequest> requests, CellStyle headerStyle, CellStyle dateStyle) {
        writeHeader(sheet, REQUEST_HEADERS, REQUEST_WIDTHS, headerStyle);

        int rowIndex = 1;
        for (ParkingRequest request : requests) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(request.getId());
            row.createCell(1).setCellValue(request.getUserId());
            row.createCell(2).setCellValue(request.getLicensePlate());
            row.createCell(3).setCellValue(request.getSpotNumber());
            setDate(row.createCell(4), request.getStartTime(), dateStyle);
            setDate(row.createCell(5), request.getEndTime(), dateStyle);
            row.createCell(6).setCellValue(request.getStatus().name());
            setDate(row.createCell(7), request.getCreatedAt(), dateStyle);
        }
    }

    private static void writeHeader(Sheet sheet, String[] headers, int[] widths, CellStyle headerStyle) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, widths[i] * 256);
        }
        sheet.createFreezePane(0, 1);
    }

    private static void setDate(Cell cell, LocalDateTime value, CellStyle dateStyle) {
        cell.setCellValue(value);
        cell.setCellStyle(dateStyle);
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private static CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat(DATE_FORMAT));
        return style;
    }
}

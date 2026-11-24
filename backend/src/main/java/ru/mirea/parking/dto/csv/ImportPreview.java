package ru.mirea.parking.dto.csv;

import java.time.Instant;
import java.util.List;

/** Результат проверки файла без записи в базу: importToken нужен для POST .../import/apply. */
public record ImportPreview(
        int totalRows,
        int validRows,
        int errorRows,
        List<RowError> errors,
        String importToken,
        Instant expiresAt) {

    public record RowError(int row, String message) {
    }
}

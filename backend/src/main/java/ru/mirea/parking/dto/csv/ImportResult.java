package ru.mirea.parking.dto.csv;

import ru.mirea.parking.csv.ImportMode;

import java.util.List;

/** Итог применения импорта: сколько создано, обновлено, пропущено и ошибки по строкам. */
public record ImportResult(
        ImportMode mode,
        int created,
        int updated,
        int skipped,
        List<ImportPreview.RowError> errors) {
}

package ru.mirea.parking.csv;

/** Режим применения импорта: только новые записи или новые плюс обновление существующих по ключу. */
public enum ImportMode {
    CREATE_ONLY,
    UPSERT
}

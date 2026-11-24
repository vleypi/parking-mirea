package ru.mirea.parking.csv;

/** Ошибка в одной строке CSV: строка попадает в список ошибок импорта, остальные обрабатываются. */
public class CsvRowException extends RuntimeException {

    public CsvRowException(String message) {
        super(message);
    }
}

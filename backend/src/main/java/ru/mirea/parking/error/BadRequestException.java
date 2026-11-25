package ru.mirea.parking.error;

import org.springframework.http.HttpStatus;

/** 400: неверные данные запроса, которые не проверить аннотациями Bean Validation. */
public class BadRequestException extends ApiException {

    private final String field;

    public BadRequestException(String message) {
        this(null, message);
    }

    public BadRequestException(String field, String message) {
        super(HttpStatus.BAD_REQUEST, ErrorCodes.BAD_REQUEST, message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}

package ru.mirea.parking.error;

import org.springframework.http.HttpStatus;

/** 404: запись не найдена. */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, ErrorCodes.NOT_FOUND, message);
    }
}

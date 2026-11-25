package ru.mirea.parking.error;

import org.springframework.http.HttpStatus;

/** 409: нарушено бизнес-правило или уникальность. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, ErrorCodes.CONFLICT, message);
    }
}

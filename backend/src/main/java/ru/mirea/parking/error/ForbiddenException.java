package ru.mirea.parking.error;

import org.springframework.http.HttpStatus;

/** 403: действие запрещено правилом, которое не выразить через роли. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, message);
    }
}

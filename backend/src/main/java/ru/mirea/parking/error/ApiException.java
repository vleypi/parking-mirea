package ru.mirea.parking.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Базовое прикладное исключение с HTTP-кодом и машинным кодом ошибки. */
public abstract class ApiException extends ResponseStatusException {

    private final String error;

    protected ApiException(HttpStatus status, String error, String message) {
        super(status, message);
        this.error = error;
    }

    public String getError() {
        return error;
    }

    @Override
    public String getMessage() {
        return getReason();
    }
}

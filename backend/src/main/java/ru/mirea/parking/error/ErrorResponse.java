package ru.mirea.parking.error;

import ru.mirea.parking.logging.CorrelationId;

import java.time.Instant;
import java.util.List;

/** Единый формат ошибки REST API. */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(int status, String error, String message, String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, error, message, path, CorrelationId.current(),
                fieldErrors == null ? List.of() : List.copyOf(fieldErrors));
    }

    public static ErrorResponse of(int status, String error, String message, String path) {
        return of(status, error, message, path, List.of());
    }
}

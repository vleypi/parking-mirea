package ru.mirea.parking.dto;

import java.util.List;
import java.util.UUID;

/** Итог массовой операции: успешные UUID и ошибки с причиной по каждой записи. */
public record BulkResponse(List<UUID> succeeded, List<Failure> failed) {

    public record Failure(UUID id, String reason) {
    }
}

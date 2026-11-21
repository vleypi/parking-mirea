package ru.mirea.parking.dto.owner;

import java.util.List;
import java.util.UUID;

/** Итог массовой операции: успешные UUID и отдельно ошибки с причиной по каждой записи. */
public record OwnerBulkResponse(List<UUID> succeeded, List<Failure> failed) {

    public record Failure(UUID id, String reason) {
    }
}

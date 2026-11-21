package ru.mirea.parking.dto.request;

import java.util.List;
import java.util.UUID;

public record RequestBulkResponse(List<UUID> succeeded, List<Failure> failed) {

    public record Failure(UUID id, String reason) {
    }
}

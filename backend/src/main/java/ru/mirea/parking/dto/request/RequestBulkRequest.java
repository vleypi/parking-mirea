package ru.mirea.parking.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record RequestBulkRequest(
        @NotEmpty @Size(max = RequestBulkRequest.MAX_IDS) List<@NotNull UUID> ids,
        @NotNull BulkAction action,
        @Size(max = 20) String status,
        @Size(max = 50) String tag) {

    public static final int MAX_IDS = 200;
}

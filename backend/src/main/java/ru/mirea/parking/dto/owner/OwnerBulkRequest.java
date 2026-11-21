package ru.mirea.parking.dto.owner;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.mirea.parking.domain.OwnerStatus;
import ru.mirea.parking.dto.request.BulkAction;

import java.util.List;
import java.util.UUID;

/**
 * POST /api/owners/bulk. status нужен для CHANGE_STATUS, tag — для ADD_TAG и REMOVE_TAG.
 * Пакет не больше {@link #MAX_IDS} записей.
 */
public record OwnerBulkRequest(
        @NotEmpty @Size(max = OwnerBulkRequest.MAX_IDS) List<@NotNull UUID> ids,
        @NotNull BulkAction action,
        OwnerStatus status,
        @Size(max = 50) String tag) {

    public static final int MAX_IDS = 200;
}

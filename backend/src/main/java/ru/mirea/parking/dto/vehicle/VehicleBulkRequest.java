package ru.mirea.parking.dto.vehicle;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.mirea.parking.domain.VehicleStatus;
import ru.mirea.parking.dto.request.BulkAction;

import java.util.List;
import java.util.UUID;

/** Тело POST /api/vehicles/bulk: status нужен для CHANGE_STATUS, tag — для ADD_TAG и REMOVE_TAG. */
public record VehicleBulkRequest(
        @NotEmpty @Size(max = VehicleBulkRequest.MAX_IDS) List<@NotNull UUID> ids,
        @NotNull BulkAction action,
        VehicleStatus status,
        @Size(max = 50) String tag) {

    public static final int MAX_IDS = 200;
}

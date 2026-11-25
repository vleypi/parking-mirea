package ru.mirea.parking.dto.vehicle;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import ru.mirea.parking.domain.VehicleStatus;

import java.util.Set;
import java.util.UUID;

/** Тело PUT /api/vehicles/{id}; version из карточки, при несовпадении — 409. */
public record VehicleUpdateRequest(
        @NotNull UUID ownerId,
        @NotBlank @ValidLicensePlate String licensePlate,
        @NotBlank @Size(max = 50) String brand,
        @NotBlank @Size(max = 50) String model,
        @Size(max = 30) String color,
        @Min(value = 1980, message = "Год выпуска не раньше 1980") @Max(9999) Short releaseYear,
        @NotNull Boolean electric,
        @NotNull VehicleStatus status,
        @Size(max = 20) Set<@NotBlank @Size(max = 50) String> tags,
        @NotNull @PositiveOrZero Long version) {
}

package ru.mirea.parking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record RequestUpdateRequest(
        @NotNull UUID vehicleId,
        @NotNull UUID spotId,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        @NotBlank @Size(max = 20) String status,
        @Size(max = 500) String description,
        @NotNull Boolean paid,
        @Size(max = 20) Set<@NotBlank @Size(max = 50) String> tags,
        @NotNull @PositiveOrZero Long version) {
}

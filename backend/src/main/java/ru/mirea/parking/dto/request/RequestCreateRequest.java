package ru.mirea.parking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record RequestCreateRequest(
        @NotNull UUID ownerId,
        @NotNull UUID vehicleId,
        @NotNull UUID spotId,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        @Size(max = 500) String description,
        Boolean paid,
        @Size(max = 20) Set<@NotBlank @Size(max = 50) String> tags) {
}

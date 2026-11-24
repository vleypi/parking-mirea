package ru.mirea.parking.dto.spot;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import ru.mirea.parking.domain.SpotStatus;

import java.math.BigDecimal;
import java.util.Set;

import static ru.mirea.parking.dto.spot.SpotCreateRequest.RATE_MESSAGE;

/** Тело PUT /api/spots/{id}; version из карточки, при несовпадении — 409. */
public record SpotUpdateRequest(
        @NotNull @Positive(message = "Номер места должен быть положительным") Integer spotNumber,
        @NotNull(message = "Укажите тип места") Short spotTypeId,
        @NotNull @DecimalMin(value = "0.00", message = RATE_MESSAGE) @DecimalMax(value = "99999.99", message = RATE_MESSAGE)
        @Digits(integer = 5, fraction = 2, message = RATE_MESSAGE) BigDecimal hourlyRate,
        @Size(max = 20, message = "Зона не длиннее 20 символов") String zone,
        @Size(max = 500) String description,
        @NotNull SpotStatus status,
        @NotNull Boolean covered,
        @Size(max = 20) Set<@NotBlank @Size(max = 50) String> tags,
        @NotNull @PositiveOrZero Long version) {
}

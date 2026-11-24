package ru.mirea.parking.dto.spot;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ru.mirea.parking.domain.SpotStatus;

import java.math.BigDecimal;
import java.util.Set;

/** Тело POST /api/spots. Статус по умолчанию AVAILABLE, крытое — нет. */
public record SpotCreateRequest(
        @NotNull @Positive(message = "Номер места должен быть положительным") Integer spotNumber,
        @NotNull(message = "Укажите тип места") Short spotTypeId,
        @NotNull @DecimalMin(value = "0.00", message = SpotCreateRequest.RATE_MESSAGE)
        @DecimalMax(value = "99999.99", message = SpotCreateRequest.RATE_MESSAGE)
        @Digits(integer = 5, fraction = 2, message = SpotCreateRequest.RATE_MESSAGE) BigDecimal hourlyRate,
        @Size(max = 20, message = "Зона не длиннее 20 символов") String zone,
        @Size(max = 500) String description,
        SpotStatus status,
        Boolean covered,
        @Size(max = 20) Set<@NotBlank @Size(max = 50) String> tags) {

    public static final String RATE_MESSAGE = "Тариф от 0 до 99999.99, не больше двух знаков после запятой";
}

package ru.mirea.parking.dto.owner;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import ru.mirea.parking.domain.OwnerStatus;

import java.math.BigDecimal;
import java.util.Set;

/** Тело PUT /api/owners/{id}. version из карточки: если запись уже изменили, ответ 409. */
public record OwnerUpdateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = OwnerConstraints.PHONE_REGEX, message = OwnerConstraints.PHONE_MESSAGE) String phone,
        @Email(message = OwnerConstraints.EMAIL_MESSAGE) @Size(max = 100) String email,
        @NotNull Short categoryId,
        @NotNull OwnerStatus status,
        @NotNull
        @DecimalMin(value = "0", message = OwnerConstraints.DISCOUNT_MESSAGE)
        @DecimalMax(value = "50", message = OwnerConstraints.DISCOUNT_MESSAGE)
        @Digits(integer = 2, fraction = 2) BigDecimal discountPercent,
        @NotNull Boolean notificationsEnabled,
        @Size(max = 20) Set<@NotBlank @Size(max = 50) String> tags,
        @NotNull @PositiveOrZero Long version) {
}

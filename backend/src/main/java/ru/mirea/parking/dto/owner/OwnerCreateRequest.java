package ru.mirea.parking.dto.owner;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.mirea.parking.domain.OwnerStatus;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Тело POST /api/owners. Необязательные поля получают значения по умолчанию:
 * статус ACTIVE, скидка 0, уведомления включены.
 */
public record OwnerCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = OwnerConstraints.PHONE_REGEX, message = OwnerConstraints.PHONE_MESSAGE) String phone,
        @Email(message = OwnerConstraints.EMAIL_MESSAGE) @Size(max = 100) String email,
        @NotNull Short categoryId,
        OwnerStatus status,
        @DecimalMin(value = "0", message = OwnerConstraints.DISCOUNT_MESSAGE)
        @DecimalMax(value = "50", message = OwnerConstraints.DISCOUNT_MESSAGE)
        @Digits(integer = 2, fraction = 2) BigDecimal discountPercent,
        Boolean notificationsEnabled,
        @Size(max = 20) Set<@NotBlank @Size(max = 50) String> tags) {
}

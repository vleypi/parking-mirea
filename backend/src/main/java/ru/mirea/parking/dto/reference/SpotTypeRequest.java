package ru.mirea.parking.dto.reference;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Тело POST и PUT /api/references/spot-types. */
public record SpotTypeRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{1,19}$",
                message = "Код из латинских букв, цифр и _, от 2 до 20 символов, начинается с буквы")
        String code,
        @NotBlank @Size(max = 50) String title,
        @Min(0) @Max(9999) Short sortOrder) {
}

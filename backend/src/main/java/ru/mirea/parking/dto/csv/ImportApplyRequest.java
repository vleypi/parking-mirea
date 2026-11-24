package ru.mirea.parking.dto.csv;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.mirea.parking.csv.ImportMode;

/** Тело POST .../import/apply. */
public record ImportApplyRequest(@NotBlank String importToken, @NotNull ImportMode mode) {
}

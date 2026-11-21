package ru.mirea.parking.dto.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record OwnerNoteUpdateRequest(
        @NotBlank @Size(max = 4000) String body,
        @NotNull Boolean pinned,
        @NotNull @PositiveOrZero Long version) {
}

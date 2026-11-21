package ru.mirea.parking.dto.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OwnerNoteCreateRequest(
        @NotBlank @Size(max = 4000) String body,
        Boolean pinned) {
}

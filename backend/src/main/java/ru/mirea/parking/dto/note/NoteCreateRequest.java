package ru.mirea.parking.dto.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Тело POST .../notes для автомобилей и мест. */
public record NoteCreateRequest(
        @NotBlank @Size(max = 4000) String body,
        Boolean pinned) {
}

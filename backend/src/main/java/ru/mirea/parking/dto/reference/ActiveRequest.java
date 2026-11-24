package ru.mirea.parking.dto.reference;

import jakarta.validation.constraints.NotNull;

/** Тело PATCH .../active: включить или отключить запись справочника. */
public record ActiveRequest(@NotNull Boolean active) {
}

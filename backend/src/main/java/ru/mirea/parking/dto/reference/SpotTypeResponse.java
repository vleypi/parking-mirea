package ru.mirea.parking.dto.reference;

import ru.mirea.parking.domain.SpotType;

/** Тип места в ответах API. */
public record SpotTypeResponse(Short id, String code, String title, boolean active, short sortOrder) {

    public static SpotTypeResponse from(SpotType type) {
        return new SpotTypeResponse(type.getId(), type.getCode(), type.getTitle(), type.isActive(), type.getSortOrder());
    }
}

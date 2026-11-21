package ru.mirea.parking.dto.reference;

import ru.mirea.parking.domain.ClientCategory;

public record ClientCategoryResponse(Short id, String code, String title, boolean active) {

    public static ClientCategoryResponse from(ClientCategory category) {
        return new ClientCategoryResponse(category.getId(), category.getCode(), category.getTitle(), category.isActive());
    }
}

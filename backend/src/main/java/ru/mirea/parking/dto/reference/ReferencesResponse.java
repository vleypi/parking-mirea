package ru.mirea.parking.dto.reference;

import java.util.List;

/** Все справочники для форм и фильтров одним ответом GET /api/references. */
public record ReferencesResponse(
        List<SpotTypeResponse> spotTypes,
        List<CodeTitle> requestStatuses,
        List<ClientCategoryResponse> clientCategories,
        List<CodeTitle> ownerStatuses,
        List<CodeTitle> vehicleStatuses,
        List<CodeTitle> spotStatuses) {

    public record CodeTitle(String code, String title) {
    }
}

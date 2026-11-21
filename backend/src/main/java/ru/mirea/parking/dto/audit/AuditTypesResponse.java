package ru.mirea.parking.dto.audit;

import ru.mirea.parking.domain.AuditEntityType;
import ru.mirea.parking.domain.AuditEventType;
import ru.mirea.parking.dto.audit.AuditEventResponse.CodeTitle;

import java.util.Arrays;
import java.util.List;

/** Справочник значений для фильтров страницы «Аудит». */
public record AuditTypesResponse(List<CodeTitle> eventTypes, List<CodeTitle> entityTypes) {

    public static AuditTypesResponse all() {
        return new AuditTypesResponse(
                Arrays.stream(AuditEventType.values()).map(t -> new CodeTitle(t.name(), t.getTitle())).toList(),
                Arrays.stream(AuditEntityType.values()).map(t -> new CodeTitle(t.name(), t.getTitle())).toList());
    }
}

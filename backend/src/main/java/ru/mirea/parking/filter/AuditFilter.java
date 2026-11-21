package ru.mirea.parking.filter;

import org.springframework.format.annotation.DateTimeFormat;
import ru.mirea.parking.domain.AuditEntityType;
import ru.mirea.parking.domain.AuditEventType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Параметры поиска по журналу аудита: GET /api/admin/audit?q=...&eventType=...&entityType=...&from=...&to=...
 *
 * @param q             текст: логин автора, описание события или correlation id
 * @param eventType     тип события
 * @param entityType    тип сущности
 * @param entityId      UUID конкретной записи (история одной заявки, владельца и т.д.)
 * @param correlationId все события одного HTTP-запроса
 * @param from          начало периода, местное время сервера (например 2026-11-20T00:00)
 * @param to            конец периода, местное время сервера
 */
public record AuditFilter(
        String q,
        AuditEventType eventType,
        AuditEntityType entityType,
        UUID entityId,
        String correlationId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
}

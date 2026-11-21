package ru.mirea.parking.dto.audit;

import ru.mirea.parking.domain.AuditEvent;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        Instant occurredAt,
        Actor actor,
        CodeTitle eventType,
        CodeTitle entityType,
        UUID entityId,
        String summary,
        String correlationId) {

    public record Actor(UUID id, String login) {
    }

    public record CodeTitle(String code, String title) {
    }

    public static AuditEventResponse from(AuditEvent event) {
        Actor actor = event.getActorId() == null && event.getActorLogin() == null
                ? null
                : new Actor(event.getActorId(), event.getActorLogin());
        CodeTitle entityType = event.getEntityType() == null
                ? null
                : new CodeTitle(event.getEntityType().name(), event.getEntityType().getTitle());
        return new AuditEventResponse(event.getId(), event.getOccurredAt(), actor,
                new CodeTitle(event.getEventType().name(), event.getEventType().getTitle()),
                entityType, event.getEntityId(), event.getSummary(), event.getCorrelationId());
    }
}

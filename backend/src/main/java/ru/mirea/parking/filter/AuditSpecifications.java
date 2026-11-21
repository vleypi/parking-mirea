package ru.mirea.parking.filter;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.parking.domain.AuditEntityType;
import ru.mirea.parking.domain.AuditEvent;
import ru.mirea.parking.domain.AuditEventType;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Условия поиска по журналу аудита. Каждое условие отдельный Specification, все складываются через AND.
 * Пользовательский текст попадает в запрос только параметром LIKE с экранированием, без конкатенации SQL.
 */
public final class AuditSpecifications {

    private AuditSpecifications() {
    }

    public static Specification<AuditEvent> byFilter(AuditFilter filter, ZoneId zone) {
        List<Specification<AuditEvent>> specs = new ArrayList<>();
        if (hasText(filter.q())) {
            specs.add(textSearch(filter.q()));
        }
        if (filter.eventType() != null) {
            specs.add(hasEventType(filter.eventType()));
        }
        if (filter.entityType() != null) {
            specs.add(hasEntityType(filter.entityType()));
        }
        if (filter.entityId() != null) {
            specs.add(hasEntityId(filter.entityId()));
        }
        if (hasText(filter.correlationId())) {
            specs.add(hasCorrelationId(filter.correlationId().trim()));
        }
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new ResponseStatusException(BAD_REQUEST, "Параметр from должен быть не позже to");
        }
        if (filter.from() != null) {
            specs.add(occurredFrom(filter.from().atZone(zone).toInstant()));
        }
        if (filter.to() != null) {
            specs.add(occurredTo(filter.to().atZone(zone).toInstant()));
        }
        return Specification.allOf(specs);
    }

    public static Specification<AuditEvent> textSearch(String text) {
        String pattern = LikePattern.contains(text);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("actorLogin")), pattern, LikePattern.ESCAPE),
                cb.like(cb.lower(root.get("summary")), pattern, LikePattern.ESCAPE),
                cb.like(cb.lower(root.get("correlationId")), pattern, LikePattern.ESCAPE));
    }

    public static Specification<AuditEvent> hasEventType(AuditEventType type) {
        return (root, query, cb) -> cb.equal(root.get("eventType"), type);
    }

    public static Specification<AuditEvent> hasEntityType(AuditEntityType type) {
        return (root, query, cb) -> cb.equal(root.get("entityType"), type);
    }

    public static Specification<AuditEvent> hasEntityId(UUID id) {
        return (root, query, cb) -> cb.equal(root.get("entityId"), id);
    }

    public static Specification<AuditEvent> hasCorrelationId(String correlationId) {
        return (root, query, cb) -> cb.equal(root.get("correlationId"), correlationId);
    }

    public static Specification<AuditEvent> occurredFrom(Instant from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from);
    }

    public static Specification<AuditEvent> occurredTo(Instant to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), to);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

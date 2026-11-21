package ru.mirea.parking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    private UUID id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "actor_login", length = 50, updatable = false)
    private String actorLogin;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30, updatable = false)
    private AuditEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 30, updatable = false)
    private AuditEntityType entityType;

    @Column(name = "entity_id", updatable = false)
    private UUID entityId;

    @Column(nullable = false, length = 500, updatable = false)
    private String summary;

    @Column(name = "correlation_id", length = 64, updatable = false)
    private String correlationId;

    protected AuditEvent() {
    }

    public AuditEvent(UUID actorId, String actorLogin, AuditEventType eventType, AuditEntityType entityType,
                      UUID entityId, String summary, String correlationId) {
        this.id = UUID.randomUUID();
        this.actorId = actorId;
        this.actorLogin = actorLogin;
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.summary = summary;
        this.correlationId = correlationId;
    }

    @PrePersist
    protected void onCreate() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public Instant getOccurredAt() { return occurredAt; }
    public UUID getActorId() { return actorId; }
    public String getActorLogin() { return actorLogin; }
    public AuditEventType getEventType() { return eventType; }
    public AuditEntityType getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getSummary() { return summary; }
    public String getCorrelationId() { return correlationId; }
}

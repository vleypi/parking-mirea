-- V3: журнал аудита.
-- Одна строка = одно событие: вход, создание, изменение, архивирование, восстановление, удаление.
-- actor_id не связан внешним ключом с user_account: запись журнала должна оставаться,
-- даже если учётную запись потом удалят, поэтому рядом хранится и логин на момент события.
CREATE TABLE audit_event (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    actor_id       UUID,
    actor_login    VARCHAR(50),
    event_type     VARCHAR(30)  NOT NULL,
    entity_type    VARCHAR(30),
    entity_id      UUID,
    summary        VARCHAR(500) NOT NULL,
    correlation_id VARCHAR(64)
);

-- Журнал смотрят по времени (новые сверху) и фильтруют по типу события и сущности.
CREATE INDEX idx_audit_event_occurred_at ON audit_event (occurred_at DESC);
CREATE INDEX idx_audit_event_type        ON audit_event (event_type);
CREATE INDEX idx_audit_event_entity      ON audit_event (entity_type, entity_id);
CREATE INDEX idx_audit_event_correlation ON audit_event (correlation_id);

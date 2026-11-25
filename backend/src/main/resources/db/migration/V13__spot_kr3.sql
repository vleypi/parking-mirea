-- V13: парковочные места (Spot) для КР3.
-- Таблица spots из КР1 дополняется столбцами; у каждого нового столбца есть DEFAULT,
-- поэтому JavaFX-версия КР2 продолжает вставлять места своим INSERT (spot_number, spot_type_id, hourly_rate).
-- uuid у spots уже добавлен миграцией V9. Внешний идентификатор для импорта — номер места (он уникален).

ALTER TABLE spots
    -- Зона или уровень парковки: «A», «Уровень -1».
    ADD COLUMN zone        VARCHAR(20),
    ADD COLUMN description VARCHAR(500),
    -- AVAILABLE: работает, MAINTENANCE: на обслуживании, CLOSED: закрыто. Заявки принимаются только в AVAILABLE.
    ADD COLUMN status      VARCHAR(15) NOT NULL DEFAULT 'AVAILABLE'
        CONSTRAINT chk_spots_status CHECK (status IN ('AVAILABLE', 'MAINTENANCE', 'CLOSED')),
    ADD COLUMN covered     BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    ADD COLUMN updated_at  TIMESTAMP   NOT NULL DEFAULT now(),
    ADD COLUMN version     BIGINT      NOT NULL DEFAULT 0,
    ADD COLUMN archived    BOOLEAN     NOT NULL DEFAULT FALSE;

-- Тариф ограничен сверху, как в SpotService КР1 (99999.99 и так предел NUMERIC(8, 2)).
ALTER TABLE spots ADD CONSTRAINT chk_spots_hourly_rate_max CHECK (hourly_rate <= 99999.99);

CREATE TABLE spot_tag (
    spot_id BIGINT      NOT NULL REFERENCES spots (id) ON DELETE CASCADE,
    tag     VARCHAR(50) NOT NULL,
    PRIMARY KEY (spot_id, tag)
);

CREATE TABLE spot_note (
    id         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    spot_id    BIGINT        NOT NULL REFERENCES spots (id) ON DELETE CASCADE,
    author_id  UUID          NOT NULL REFERENCES user_account (id),
    body       VARCHAR(4000) NOT NULL,
    pinned     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at TIMESTAMP     NOT NULL DEFAULT now(),
    version    BIGINT        NOT NULL DEFAULT 0
);

-- spot_number и uuid уже проиндексированы через UNIQUE.
CREATE INDEX idx_spots_type     ON spots (spot_type_id);
CREATE INDEX idx_spots_status   ON spots (status);
CREATE INDEX idx_spots_archived ON spots (archived);
CREATE INDEX idx_spot_tag_tag   ON spot_tag (tag);
CREATE INDEX idx_spot_note_spot ON spot_note (spot_id, pinned DESC, created_at DESC);
-- Доступность и свободные места: заявки места, пересекающие момент или период.
CREATE INDEX idx_requests_spot_period ON requests (spot_id, start_time, end_time);

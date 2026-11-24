-- V12: автомобили (Vehicle) для КР3.
-- Таблица vehicles из КР1 дополняется столбцами; у каждого нового столбца есть DEFAULT,
-- поэтому JavaFX-версия КР2 продолжает вставлять машины своим INSERT (user_id, license_plate, brand, model).
-- uuid у vehicles уже добавлен миграцией V9. Внешний идентификатор для импорта — гос. номер (он уникален).

ALTER TABLE vehicles
    ADD COLUMN color        VARCHAR(30),
    -- ACTIVE: обычная работа, BLOCKED: запрет въезда, DEREGISTERED: снят с учёта.
    ADD COLUMN status       VARCHAR(15) NOT NULL DEFAULT 'ACTIVE'
        CONSTRAINT chk_vehicles_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'DEREGISTERED')),
    -- Верхнюю границу (не позже текущего года) проверяет сервис: CHECK с текущей датой в PostgreSQL не рекомендуется.
    ADD COLUMN release_year SMALLINT
        CONSTRAINT chk_vehicles_release_year CHECK (release_year >= 1980),
    ADD COLUMN is_electric  BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN created_at   TIMESTAMP   NOT NULL DEFAULT now(),
    ADD COLUMN updated_at   TIMESTAMP   NOT NULL DEFAULT now(),
    ADD COLUMN version      BIGINT      NOT NULL DEFAULT 0,
    ADD COLUMN archived     BOOLEAN     NOT NULL DEFAULT FALSE;

-- Старые данные КР1: Tesla — электромобиль.
UPDATE vehicles SET is_electric = TRUE WHERE lower(brand) = 'tesla';

CREATE TABLE vehicle_tag (
    vehicle_id BIGINT      NOT NULL REFERENCES vehicles (id) ON DELETE CASCADE,
    tag        VARCHAR(50) NOT NULL,
    PRIMARY KEY (vehicle_id, tag)
);

CREATE TABLE vehicle_note (
    id         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id BIGINT        NOT NULL REFERENCES vehicles (id) ON DELETE CASCADE,
    author_id  UUID          NOT NULL REFERENCES user_account (id),
    body       VARCHAR(4000) NOT NULL,
    pinned     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at TIMESTAMP     NOT NULL DEFAULT now(),
    version    BIGINT        NOT NULL DEFAULT 0
);

-- license_plate и uuid уже проиндексированы через UNIQUE.
CREATE INDEX idx_vehicles_brand    ON vehicles (brand);
CREATE INDEX idx_vehicles_user     ON vehicles (user_id);
CREATE INDEX idx_vehicles_archived ON vehicles (archived);
CREATE INDEX idx_vehicle_tag_tag   ON vehicle_tag (tag);
CREATE INDEX idx_vehicle_note_vehicle ON vehicle_note (vehicle_id, pinned DESC, created_at DESC);
-- Подсчёт активности: заявки машины за период.
CREATE INDEX idx_requests_vehicle_start ON requests (vehicle_id, start_time);

-- V10: заявки на парковку (Request) для КР3.
-- Таблица requests из КР1 не пересоздаётся, а дополняется новыми столбцами.
-- У каждого нового столбца есть DEFAULT, поэтому JavaFX-версия КР2 продолжает
-- вставлять заявки своим INSERT (user_id, vehicle_id, spot_id, start_time, end_time, status_id, created_at).

-- 1. Номер заявки REQ-000001 берётся из отдельной последовательности.
CREATE SEQUENCE request_code_seq;

-- 2. Новые столбцы. Сначала без NOT NULL у code, чтобы успеть заполнить старые строки.
ALTER TABLE requests
    -- Внешний идентификатор для REST API: по нему ищем /api/requests/{id}.
    -- gen_random_uuid() вычисляется для каждой строки отдельно, старые заявки тоже получают свой UUID.
    ADD COLUMN uuid        UUID          NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN code        VARCHAR(20),
    ADD COLUMN description VARCHAR(500),
    ADD COLUMN cost        NUMERIC(10, 2) NOT NULL DEFAULT 0 CHECK (cost >= 0),
    ADD COLUMN paid        BOOLEAN       NOT NULL DEFAULT FALSE,
    ADD COLUMN updated_at  TIMESTAMP     NOT NULL DEFAULT now(),
    -- Счётчик для optimistic locking (@Version): каждое изменение через JPA увеличивает его на 1.
    ADD COLUMN version     BIGINT        NOT NULL DEFAULT 0,
    -- Архив вместо удаления.
    ADD COLUMN archived    BOOLEAN       NOT NULL DEFAULT FALSE;

-- 3. Старые данные КР1.
-- Номер заявки строим из id, чтобы у старых заявок он был предсказуемым: id 7 -> REQ-000007.
-- Стоимость = длительность в часах * тариф места. EXTRACT(EPOCH ...) даёт секунды.
-- Завершённые заявки (status_id = 3) считаем оплаченными.
UPDATE requests r
SET code       = 'REQ-' || lpad(r.id::text, 6, '0'),
    cost       = round(EXTRACT(EPOCH FROM (r.end_time - r.start_time)) / 3600 * s.hourly_rate, 2),
    paid       = (r.status_id = 3),
    updated_at = r.created_at
FROM spots s
WHERE s.id = r.spot_id;

-- 4. Последовательность продолжает нумерацию после самой большой старой заявки.
-- Третий аргумент false значит «следующий nextval вернёт ровно это значение».
SELECT setval('request_code_seq', COALESCE((SELECT max(id) FROM requests), 0) + 1, false);

-- 5. Теперь, когда старые строки заполнены, задаём DEFAULT и ограничения для code.
ALTER TABLE requests
    ALTER COLUMN code SET DEFAULT 'REQ-' || lpad(nextval('request_code_seq')::text, 6, '0'),
    ALTER COLUMN code SET NOT NULL,
    ADD CONSTRAINT uq_requests_uuid UNIQUE (uuid),
    ADD CONSTRAINT uq_requests_code UNIQUE (code);

-- Последовательность принадлежит столбцу: удалят столбец, удалится и она.
ALTER SEQUENCE request_code_seq OWNED BY requests.code;

-- 6. Теги заявки (@ElementCollection в сущности Request).
-- ON DELETE CASCADE: при окончательном удалении заявки её теги удаляются автоматически.
CREATE TABLE request_tag (
    request_id BIGINT      NOT NULL REFERENCES requests (id) ON DELETE CASCADE,
    tag        VARCHAR(50) NOT NULL,
    PRIMARY KEY (request_id, tag)
);

-- 7. Заметки к заявке (используются в задаче «Заметки к заявке»).
-- author_id ссылается на учётную запись user_account из миграции V2 (безопасность).
-- Внешний ключ на user_account добавит миграция задачи «Заметки», когда V2 уже будет в main.
CREATE TABLE request_note (
    id         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id BIGINT        NOT NULL REFERENCES requests (id) ON DELETE CASCADE,
    author_id  UUID          NOT NULL,
    body       VARCHAR(4000) NOT NULL,
    pinned     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at TIMESTAMP     NOT NULL DEFAULT now(),
    version    BIGINT        NOT NULL DEFAULT 0
);

-- 8. Индексы на поля поиска и фильтров. code и uuid уже проиндексированы через UNIQUE.
CREATE INDEX idx_requests_status     ON requests (status_id);
CREATE INDEX idx_requests_start_time ON requests (start_time);
CREATE INDEX idx_requests_archived   ON requests (archived);
CREATE INDEX idx_request_tag_tag     ON request_tag (tag);
-- Заметки выводятся так: сначала закреплённые, потом новые.
CREATE INDEX idx_request_note_request ON request_note (request_id, pinned DESC, created_at DESC);

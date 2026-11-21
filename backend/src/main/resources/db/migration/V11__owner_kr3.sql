-- V11: владельцы (сущность Owner, таблица users) для КР3.
-- Таблица users из КР1 не пересоздаётся, а дополняется новыми столбцами.
-- У каждого нового столбца есть DEFAULT, поэтому JavaFX-версия КР2 продолжает
-- вставлять владельцев своим INSERT (name, phone, created_at) и ничего не знает о новых полях.
-- uuid у users уже добавлен миграцией V9.

-- 1. Справочник категорий клиентов. id задаём явно: id 1 «Обычный» используется как DEFAULT в users.
CREATE TABLE client_categories (
    id     SMALLINT    PRIMARY KEY,
    code   VARCHAR(20) NOT NULL UNIQUE,
    title  VARCHAR(50) NOT NULL UNIQUE,
    -- Отключённую категорию нельзя выбрать для нового владельца, но старые записи на неё ссылаются.
    active BOOLEAN     NOT NULL DEFAULT TRUE
);

INSERT INTO client_categories (id, code, title) VALUES
    (1, 'REGULAR',  'Обычный'),
    (2, 'RESIDENT', 'Резидент'),
    (3, 'EMPLOYEE', 'Сотрудник');

-- 2. Новые столбцы владельца.
ALTER TABLE users
    ADD COLUMN email                 VARCHAR(100),
    ADD COLUMN category_id           SMALLINT     NOT NULL DEFAULT 1 REFERENCES client_categories (id),
    -- ACTIVE: обычная работа, BLOCKED: на владельца нельзя оформлять заявки (например, долг).
    ADD COLUMN status                VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE'
        CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'BLOCKED')),
    ADD COLUMN discount_percent      NUMERIC(5, 2) NOT NULL DEFAULT 0
        CONSTRAINT chk_users_discount CHECK (discount_percent BETWEEN 0 AND 50),
    ADD COLUMN notifications_enabled BOOLEAN      NOT NULL DEFAULT TRUE,
    ADD COLUMN updated_at            TIMESTAMP    NOT NULL DEFAULT now(),
    -- Счётчик для optimistic locking (@Version): каждое изменение через JPA увеличивает его на 1.
    ADD COLUMN version               BIGINT       NOT NULL DEFAULT 0,
    -- Архив вместо удаления.
    ADD COLUMN archived              BOOLEAN      NOT NULL DEFAULT FALSE;

-- 3. Старые владельцы КР1: время изменения = время регистрации.
UPDATE users SET updated_at = created_at;

-- 4. Теги владельца (@ElementCollection в сущности Owner).
-- ON DELETE CASCADE: при окончательном удалении владельца его теги удаляются автоматически.
CREATE TABLE owner_tag (
    owner_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    tag      VARCHAR(50) NOT NULL,
    PRIMARY KEY (owner_id, tag)
);

-- 5. Заметки к владельцу (задача «Заметки к владельцу»). Автор — учётная запись сотрудника из V2.
CREATE TABLE owner_note (
    id         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id   BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    author_id  UUID          NOT NULL REFERENCES user_account (id),
    body       VARCHAR(4000) NOT NULL,
    pinned     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at TIMESTAMP     NOT NULL DEFAULT now(),
    version    BIGINT        NOT NULL DEFAULT 0
);

-- 6. Индексы на поля поиска и фильтров. phone и uuid уже проиндексированы через UNIQUE.
CREATE INDEX idx_users_name     ON users (name);
CREATE INDEX idx_users_category ON users (category_id);
CREATE INDEX idx_users_archived ON users (archived);
CREATE INDEX idx_owner_tag_tag  ON owner_tag (tag);
-- Заметки выводятся так: сначала закреплённые, потом новые.
CREATE INDEX idx_owner_note_owner ON owner_note (owner_id, pinned DESC, created_at DESC);

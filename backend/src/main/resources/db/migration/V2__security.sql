CREATE TABLE role (
    code  VARCHAR(20) PRIMARY KEY,
    title VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO role (code, title) VALUES
    ('VIEWER',   'Наблюдатель'),
    ('OPERATOR', 'Оператор'),
    ('ADMIN',    'Администратор');

CREATE TABLE user_account (
    id                    UUID         PRIMARY KEY,
    login                 VARCHAR(50)  NOT NULL UNIQUE,
    display_name          VARCHAR(100) NOT NULL,
    password_hash         VARCHAR(100) NOT NULL,
    enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    must_change_password  BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_attempts INTEGER      NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version               BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE user_account_role (
    user_id   UUID        NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    role_code VARCHAR(20) NOT NULL REFERENCES role (code),
    PRIMARY KEY (user_id, role_code)
);

CREATE TABLE refresh_session (
    id         UUID         PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    token_hash VARCHAR(64)  NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked_at TIMESTAMPTZ,
    user_agent VARCHAR(500),
    ip_address VARCHAR(64),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_refresh_session_user ON refresh_session (user_id);

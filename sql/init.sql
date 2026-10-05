DROP TABLE IF EXISTS requests CASCADE;
DROP TABLE IF EXISTS vehicles CASCADE;
DROP TABLE IF EXISTS spots CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS request_statuses CASCADE;
DROP TABLE IF EXISTS spot_types CASCADE;

CREATE TABLE request_statuses (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE spot_types (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE CHECK (phone ~ '^\+7-[3489][0-9]{2}-[0-9]{3}-[0-9]{2}-[0-9]{2}$'),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id),
    license_plate VARCHAR(12) NOT NULL UNIQUE CHECK (license_plate ~ '^[АВЕКМНОРСТУХ][0-9]{3}[АВЕКМНОРСТУХ]{2}[0-9]{2,3}$'),
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL
);

CREATE TABLE spots (
    id BIGSERIAL PRIMARY KEY,
    spot_number INT NOT NULL UNIQUE CHECK (spot_number > 0),
    spot_type_id SMALLINT NOT NULL REFERENCES spot_types (id),
    hourly_rate NUMERIC(8, 2) NOT NULL CHECK (hourly_rate >= 0)
);

CREATE TABLE requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id),
    vehicle_id BIGINT NOT NULL REFERENCES vehicles (id),
    spot_id BIGINT NOT NULL REFERENCES spots (id),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status_id SMALLINT NOT NULL REFERENCES request_statuses (id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CHECK (end_time > start_time)
);

-- id справочников совпадают с id в Java-enum RequestStatus и SpotType
INSERT INTO request_statuses (id, code, title) VALUES
    (1, 'NEW', 'Новая'),
    (2, 'CONFIRMED', 'Подтверждена'),
    (3, 'COMPLETED', 'Завершена'),
    (4, 'CANCELLED', 'Отменена');

INSERT INTO spot_types (id, code, title) VALUES
    (1, 'STANDARD', 'Стандартное'),
    (2, 'DISABLED', 'Для людей с инвалидностью'),
    (3, 'ELECTRIC', 'Для электромобилей');

INSERT INTO users (name, phone) VALUES
    ('Иван Иванов', '+7-900-100-00-01'),
    ('Мария Петрова', '+7-900-100-00-02'),
    ('Алексей Смирнов', '+7-900-100-00-03'),
    ('Ольга Кузнецова', '+7-900-100-00-04'),
    ('Дмитрий Соколов', '+7-900-100-00-05');

INSERT INTO vehicles (user_id, license_plate, brand, model) VALUES
    (1, 'А123ВС777', 'Toyota', 'Camry'),
    (2, 'В456КМ177', 'Kia', 'Rio'),
    (3, 'Е789НО799', 'Volkswagen', 'Polo'),
    (4, 'К012РС777', 'Hyundai', 'Solaris'),
    (5, 'Н345ТУ177', 'Skoda', 'Octavia'),
    (1, 'М777АА777', 'Tesla', 'Model 3');

-- spot_type_id: 1 стандартное, 2 для людей с инвалидностью, 3 для электромобилей
INSERT INTO spots (spot_number, spot_type_id, hourly_rate) VALUES
    (1, 1, 100.00),
    (2, 1, 100.00),
    (3, 1, 100.00),
    (4, 1, 120.00),
    (5, 3, 150.00),
    (6, 3, 150.00),
    (7, 2, 80.00),
    (8, 2, 80.00),
    (9, 1, 100.00),
    (10, 1, 120.00);

-- status_id: 1 новая, 2 подтверждена, 3 завершена, 4 отменена
INSERT INTO requests (user_id, vehicle_id, spot_id, start_time, end_time, status_id) VALUES
    (1, 1, 1, '2026-09-10 08:00', '2026-09-10 18:00', 1),
    (1, 6, 5, '2026-09-12 09:00', '2026-09-12 20:00', 2),
    (1, 1, 1, '2026-09-14 09:00', '2026-09-14 15:00', 3),
    (2, 2, 2, '2026-09-08 10:00', '2026-09-08 19:00', 3),
    (2, 2, 2, '2026-09-11 07:30', '2026-09-11 22:00', 1),
    (3, 3, 3, '2026-09-09 12:00', '2026-09-09 15:00', 4),
    (3, 3, 4, '2026-09-13 08:00', '2026-09-13 12:00', 2),
    (4, 4, 6, '2026-09-07 09:00', '2026-09-07 18:00', 3),
    (4, 4, 6, '2026-09-14 10:00', '2026-09-14 16:00', 1),
    (5, 5, 7, '2026-09-15 08:00', '2026-09-15 20:00', 2),
    (5, 5, 8, '2026-09-16 11:00', '2026-09-16 14:00', 1),
    (2, 2, 9, '2026-09-12 10:00', '2026-09-12 19:00', 4);

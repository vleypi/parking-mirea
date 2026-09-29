DROP TABLE IF EXISTS parking_requests;
DROP TABLE IF EXISTS vehicles;
DROP TABLE IF EXISTS parking_spots;
DROP TABLE IF EXISTS users;

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

CREATE TABLE parking_spots (
    id BIGSERIAL PRIMARY KEY,
    spot_number INT NOT NULL UNIQUE CHECK (spot_number > 0),
    spot_type VARCHAR(20) NOT NULL CHECK (spot_type IN ('STANDARD', 'DISABLED', 'ELECTRIC')),
    hourly_rate NUMERIC(8, 2) NOT NULL CHECK (hourly_rate >= 0)
);

CREATE TABLE parking_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id),
    vehicle_id BIGINT NOT NULL REFERENCES vehicles (id),
    spot_id BIGINT NOT NULL REFERENCES parking_spots (id),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('NEW', 'CONFIRMED', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CHECK (end_time > start_time)
);

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

INSERT INTO parking_spots (spot_number, spot_type, hourly_rate) VALUES
    (1, 'STANDARD', 100.00),
    (2, 'STANDARD', 100.00),
    (3, 'STANDARD', 100.00),
    (4, 'STANDARD', 120.00),
    (5, 'ELECTRIC', 150.00),
    (6, 'ELECTRIC', 150.00),
    (7, 'DISABLED', 80.00),
    (8, 'DISABLED', 80.00),
    (9, 'STANDARD', 100.00),
    (10, 'STANDARD', 120.00);

INSERT INTO parking_requests (user_id, vehicle_id, spot_id, start_time, end_time, status) VALUES
    (1, 1, 1, '2026-09-10 08:00', '2026-09-10 18:00', 'NEW'),
    (1, 6, 5, '2026-09-12 09:00', '2026-09-12 20:00', 'CONFIRMED'),
    (1, 1, 1, '2026-09-14 09:00', '2026-09-14 15:00', 'COMPLETED'),
    (2, 2, 2, '2026-09-08 10:00', '2026-09-08 19:00', 'COMPLETED'),
    (2, 2, 2, '2026-09-11 07:30', '2026-09-11 22:00', 'NEW'),
    (3, 3, 3, '2026-09-09 12:00', '2026-09-09 15:00', 'CANCELLED'),
    (3, 3, 4, '2026-09-13 08:00', '2026-09-13 12:00', 'CONFIRMED'),
    (4, 4, 6, '2026-09-07 09:00', '2026-09-07 18:00', 'COMPLETED'),
    (4, 4, 6, '2026-09-14 10:00', '2026-09-14 16:00', 'NEW'),
    (5, 5, 7, '2026-09-15 08:00', '2026-09-15 20:00', 'CONFIRMED'),
    (5, 5, 8, '2026-09-16 11:00', '2026-09-16 14:00', 'NEW'),
    (2, 2, 9, '2026-09-12 10:00', '2026-09-12 19:00', 'CANCELLED');

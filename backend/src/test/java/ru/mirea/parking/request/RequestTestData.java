package ru.mirea.parking.request;

import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

final class RequestTestData {

    static final LocalDateTime BASE = LocalDateTime.of(2030, 1, 10, 9, 0);

    private RequestTestData() {
    }

    static void seed(JdbcTemplate jdbc) {
        for (String table : List.of("request_tag", "requests", "vehicles", "spots", "spot_types", "users", "request_statuses")) {
            jdbc.update("delete from " + table);
        }
        jdbc.update("insert into spot_types (id, code, title) values "
                + "(1, 'STANDARD', 'Стандартное'), (2, 'DISABLED', 'Для людей с инвалидностью'), (3, 'ELECTRIC', 'Для электромобилей')");
        jdbc.update("insert into request_statuses (id, code, title) values "
                + "(1, 'NEW', 'Новая'), (2, 'CONFIRMED', 'Подтверждена'), (3, 'COMPLETED', 'Завершена'), (4, 'CANCELLED', 'Отменена')");
        jdbc.update("insert into users (id, uuid, name, phone, created_at) values "
                + "(1, ?, 'Иван Иванов', '+7-900-100-00-01', now()), (2, ?, 'Мария O''Нил_100%', '+7-900-100-00-02', now())",
                UUID.randomUUID(), UUID.randomUUID());
        jdbc.update("insert into vehicles (id, uuid, user_id, license_plate, brand, model) values "
                + "(1, ?, 1, 'А123ВС777', 'Toyota', 'Camry'), (2, ?, 2, 'В456КМ177', 'Tesla', 'Model 3')",
                UUID.randomUUID(), UUID.randomUUID());
        jdbc.update("insert into spots (id, uuid, spot_number, spot_type_id, hourly_rate) values "
                + "(1, ?, 1, 1, 100.00), (2, ?, 5, 3, 150.00)", UUID.randomUUID(), UUID.randomUUID());

        insert(jdbc, 1, "REQ-000001", 1, 1, 1, 1, BASE, "100.00", false, false);
        insert(jdbc, 2, "REQ-000002", 1, 1, 1, 2, BASE.plusDays(1), "200.00", true, false);
        insert(jdbc, 3, "REQ-000003", 2, 2, 2, 1, BASE.plusDays(2), "300.00", false, false);
        insert(jdbc, 4, "REQ-000004", 2, 2, 2, 3, BASE.plusDays(3), "450.00", true, false);
        insert(jdbc, 5, "REQ-000005", 1, 1, 2, 4, BASE.plusDays(4), "50.00", false, true);
        jdbc.update("insert into request_tag (request_id, tag) values (1, 'vip'), (3, 'vip'), (3, 'ночь'), (4, 'ночь')");
    }

    private static void insert(JdbcTemplate jdbc, long id, String code, long owner, long vehicle, long spot, int status,
                               LocalDateTime start, String cost, boolean paid, boolean archived) {
        jdbc.update("insert into requests (id, uuid, code, user_id, vehicle_id, spot_id, status_id, start_time, end_time, "
                        + "cost, paid, archived, created_at, updated_at, version) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), 0)",
                id, UUID.randomUUID(), code, owner, vehicle, spot, status, start, start.plusHours(2),
                new BigDecimal(cost), paid, archived, start.minusDays(30).plusMinutes(id));
    }
}

package ru.mirea.parking.vehicle;

import org.springframework.jdbc.core.JdbcTemplate;
import ru.mirea.parking.TestDataCleaner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Демо-данные для тестов автомобилей: три владельца (третий в архиве), одно место, справочники. */
final class VehicleTestData {

    static final LocalDateTime CREATED = LocalDateTime.of(2026, 1, 15, 10, 0);

    private VehicleTestData() {
    }

    static void seed(JdbcTemplate jdbc) {
        TestDataCleaner.clean(jdbc);
        TestDataCleaner.seedReferences(jdbc);
        jdbc.update("insert into users (id, uuid, name, phone, created_at) values "
                + "(1, ?, 'Иван Иванов', '+7-900-100-00-01', now()), (2, ?, 'Мария Петрова', '+7-900-100-00-02', now())",
                UUID.randomUUID(), UUID.randomUUID());
        jdbc.update("insert into users (id, uuid, name, phone, archived, created_at) values "
                + "(3, ?, 'Анна Архивная', '+7-900-100-00-03', true, now())", UUID.randomUUID());
        jdbc.update("insert into spots (id, uuid, spot_number, spot_type_id, hourly_rate) values (1, ?, 1, 1, 100.00)",
                UUID.randomUUID());
    }

    static UUID ownerUuid(JdbcTemplate jdbc, long id) {
        return jdbc.queryForObject("select uuid from users where id = ?", UUID.class, id);
    }

    /** Автомобиль со всеми полями КР3, возвращает его UUID. */
    static UUID vehicle(JdbcTemplate jdbc, long id, long owner, String plate, String brand, String model, Integer year,
                        boolean electric, String status, boolean archived, String... tags) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("insert into vehicles (id, uuid, user_id, license_plate, brand, model, release_year, is_electric, "
                        + "status, archived, created_at, updated_at, version) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, uuid, owner, plate, brand, model, year, electric, status, archived,
                CREATED.plusDays(id), CREATED.plusDays(id));
        for (String tag : tags) {
            jdbc.update("insert into vehicle_tag (vehicle_id, tag) values (?, ?)", id, tag);
        }
        return uuid;
    }

    /** Заявка машины: status 1 NEW, 2 CONFIRMED, 3 COMPLETED, 4 CANCELLED. */
    static void request(JdbcTemplate jdbc, long id, long vehicle, int status, LocalDateTime start) {
        long owner = jdbc.queryForObject("select user_id from vehicles where id = ?", Long.class, vehicle);
        jdbc.update("insert into requests (id, uuid, code, user_id, vehicle_id, spot_id, status_id, start_time, end_time, "
                        + "cost, paid, archived, created_at, updated_at, version) "
                        + "values (?, ?, ?, ?, ?, 1, ?, ?, ?, ?, false, false, now(), now(), 0)",
                id, UUID.randomUUID(), "REQ-%06d".formatted(id), owner, vehicle, status, start, start.plusHours(2),
                new BigDecimal("200.00"));
    }
}

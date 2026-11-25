package ru.mirea.parking.spot;

import org.springframework.jdbc.core.JdbcTemplate;
import ru.mirea.parking.TestDataCleaner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Демо-данные для тестов мест: владелец с машиной, справочники (типы 1 STANDARD, 2 DISABLED, 3 ELECTRIC). */
final class SpotTestData {

    static final LocalDateTime CREATED = LocalDateTime.of(2026, 1, 15, 10, 0);

    private SpotTestData() {
    }

    static void seed(JdbcTemplate jdbc) {
        TestDataCleaner.clean(jdbc);
        TestDataCleaner.seedReferences(jdbc);
        jdbc.update("insert into users (id, uuid, name, phone, created_at) values (1, ?, 'Иван Иванов', '+7-900-100-00-01', now())",
                UUID.randomUUID());
        jdbc.update("insert into vehicles (id, uuid, user_id, license_plate, brand, model) values (1, ?, 1, 'А123ВС777', 'Toyota', 'Camry')",
                UUID.randomUUID());
    }

    static UUID ownerUuid(JdbcTemplate jdbc) {
        return jdbc.queryForObject("select uuid from users where id = 1", UUID.class);
    }

    static UUID vehicleUuid(JdbcTemplate jdbc) {
        return jdbc.queryForObject("select uuid from vehicles where id = 1", UUID.class);
    }

    /** Место со всеми полями КР3, возвращает его UUID. */
    static UUID spot(JdbcTemplate jdbc, long id, int number, int type, String rate, String zone, String description,
                     String status, boolean covered, boolean archived, String... tags) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("insert into spots (id, uuid, spot_number, spot_type_id, hourly_rate, zone, description, status, covered, "
                        + "archived, created_at, updated_at, version) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, uuid, number, type, new BigDecimal(rate), zone, description, status, covered, archived,
                CREATED.plusDays(id), CREATED.plusDays(id));
        for (String tag : tags) {
            jdbc.update("insert into spot_tag (spot_id, tag) values (?, ?)", id, tag);
        }
        return uuid;
    }

    /** Заявка на место на 2 часа: status 1 NEW, 2 CONFIRMED, 3 COMPLETED, 4 CANCELLED. */
    static void request(JdbcTemplate jdbc, long id, long spot, int status, LocalDateTime start) {
        request(jdbc, id, spot, status, start, start.plusHours(2));
    }

    static void request(JdbcTemplate jdbc, long id, long spot, int status, LocalDateTime start, LocalDateTime end) {
        jdbc.update("insert into requests (id, uuid, code, user_id, vehicle_id, spot_id, status_id, start_time, end_time, "
                        + "cost, paid, archived, created_at, updated_at, version) "
                        + "values (?, ?, ?, 1, 1, ?, ?, ?, ?, ?, false, false, now(), now(), 0)",
                id, UUID.randomUUID(), "REQ-%06d".formatted(id), spot, status, start, end, new BigDecimal("200.00"));
    }
}

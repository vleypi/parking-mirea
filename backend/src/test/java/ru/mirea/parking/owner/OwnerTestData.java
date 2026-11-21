package ru.mirea.parking.owner;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Демо-данные для тестов владельцев. Таблицы чистятся в порядке внешних ключей. */
final class OwnerTestData {

    static final LocalDateTime REGISTERED = LocalDateTime.of(2026, 1, 15, 10, 0);

    private OwnerTestData() {
    }

    static void clean(JdbcTemplate jdbc) {
        for (String table : List.of("request_note", "request_tag", "requests", "vehicles", "spots", "spot_types",
                "owner_note", "owner_tag", "users", "request_statuses")) {
            jdbc.update("delete from " + table);
        }
        // Тестовые владельцы вставляются с явными id, новые через API получают id начиная с 1000.
        jdbc.update("alter table users alter column id restart with 1000");
        // Категории 1–3 заполняет src/test/resources/data.sql, тесты могут их менять: возвращаем исходный вид.
        jdbc.update("delete from client_categories where id > 3");
        jdbc.update("update client_categories set active = true");
    }

    /** Владелец со всеми полями КР3, возвращает его UUID. */
    static UUID insert(JdbcTemplate jdbc, long id, String name, String phone, int category, String status,
                       String discount, boolean notifications, boolean archived, String... tags) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("insert into users (id, uuid, name, phone, email, category_id, status, discount_percent, "
                        + "notifications_enabled, archived, created_at, updated_at, version) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, uuid, name, phone, null, category, status, new java.math.BigDecimal(discount), notifications, archived,
                REGISTERED.plusDays(id), REGISTERED.plusDays(id));
        for (String tag : tags) {
            jdbc.update("insert into owner_tag (owner_id, tag) values (?, ?)", id, tag);
        }
        return uuid;
    }

    /** Вставка так, как это делает JavaFX-версия КР2: только name, phone и created_at. */
    static long insertLegacy(JdbcTemplate jdbc, String name, String phone) {
        jdbc.update("insert into users (uuid, name, phone, created_at) values (?, ?, ?, ?)",
                UUID.randomUUID(), name, phone, REGISTERED);
        return jdbc.queryForObject("select id from users where phone = ?", Long.class, phone);
    }
}

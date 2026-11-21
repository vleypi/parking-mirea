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
                "owner_tag", "users", "request_statuses")) {
            jdbc.update("delete from " + table);
        }
        // Категории 1–3 заполняет src/test/resources/data.sql, тесты могут их менять: возвращаем исходный вид.
        jdbc.update("delete from client_categories where id > 3");
        jdbc.update("update client_categories set active = true");
    }

    /** Вставка так, как это делает JavaFX-версия КР2: только name, phone и created_at. */
    static long insertLegacy(JdbcTemplate jdbc, String name, String phone) {
        jdbc.update("insert into users (uuid, name, phone, created_at) values (?, ?, ?, ?)",
                UUID.randomUUID(), name, phone, REGISTERED);
        return jdbc.queryForObject("select id from users where phone = ?", Long.class, phone);
    }
}

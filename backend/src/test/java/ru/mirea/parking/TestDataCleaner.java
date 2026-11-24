package ru.mirea.parking;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/** Очистка тестовой базы H2 в порядке внешних ключей и заполнение справочников. */
public final class TestDataCleaner {

    private static final List<String> TABLES = List.of(
            "request_note", "request_tag", "requests",
            "vehicle_note", "vehicle_tag", "vehicles", "spot_note", "spot_tag", "spots", "spot_types",
            "owner_note", "owner_tag", "users", "request_statuses", "audit_event");

    private TestDataCleaner() {
    }

    public static void clean(JdbcTemplate jdbc) {
        for (String table : TABLES) {
            jdbc.update("delete from " + table);
        }
        jdbc.update("alter table users alter column id restart with 1000");
        jdbc.update("alter table vehicles alter column id restart with 1000");
        jdbc.update("alter table spots alter column id restart with 1000");
        jdbc.update("delete from client_categories where id > 3");
        jdbc.update("update client_categories set active = true");
    }

    /** Типы мест и статусы заявок как в миграциях V1 и V4. */
    public static void seedReferences(JdbcTemplate jdbc) {
        jdbc.update("insert into spot_types (id, code, title, active, sort_order) values "
                + "(1, 'STANDARD', 'Стандартное', true, 10), "
                + "(2, 'DISABLED', 'Для людей с инвалидностью', true, 20), "
                + "(3, 'ELECTRIC', 'Для электромобилей', true, 30)");
        jdbc.update("insert into request_statuses (id, code, title) values "
                + "(1, 'NEW', 'Новая'), (2, 'CONFIRMED', 'Подтверждена'), "
                + "(3, 'COMPLETED', 'Завершена'), (4, 'CANCELLED', 'Отменена')");
    }
}

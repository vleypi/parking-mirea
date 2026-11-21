-- Справочник категорий клиентов для тестовой схемы H2 (в PostgreSQL его заполняет миграция V11).
-- users.category_id по умолчанию = 1, поэтому строка «Обычный» нужна любому тесту, который вставляет владельцев.
insert into client_categories (id, code, title, active) values
    (1, 'REGULAR', 'Обычный', true),
    (2, 'RESIDENT', 'Резидент', true),
    (3, 'EMPLOYEE', 'Сотрудник', true);

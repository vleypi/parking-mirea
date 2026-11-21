package ru.mirea.parking.domain;

/**
 * Тип события журнала аудита. В базе хранится имя константы (EnumType.STRING).
 */
public enum AuditEventType {
    LOGIN_SUCCESS("Успешный вход"),
    LOGIN_FAILURE("Неуспешный вход"),
    CREATE("Создание"),
    UPDATE("Изменение"),
    ARCHIVE("Архивирование"),
    RESTORE("Восстановление"),
    DELETE("Окончательное удаление");

    private final String title;

    AuditEventType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}

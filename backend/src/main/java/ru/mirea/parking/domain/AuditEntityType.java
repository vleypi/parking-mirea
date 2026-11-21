package ru.mirea.parking.domain;

/**
 * Тип сущности, к которой относится событие. Для входа в систему это учётная запись.
 */
public enum AuditEntityType {
    REQUEST("Заявка"),
    OWNER("Владелец"),
    VEHICLE("Автомобиль"),
    SPOT("Парковочное место"),
    USER_ACCOUNT("Учётная запись"),
    REFERENCE("Справочник");

    private final String title;

    AuditEntityType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}

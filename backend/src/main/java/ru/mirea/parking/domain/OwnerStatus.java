package ru.mirea.parking.domain;

/** Статус владельца. В базе хранится имя константы (users.status). */
public enum OwnerStatus {
    ACTIVE("Активен"),
    BLOCKED("Заблокирован");

    private final String title;

    OwnerStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}

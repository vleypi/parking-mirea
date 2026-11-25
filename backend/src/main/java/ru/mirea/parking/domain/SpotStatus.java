package ru.mirea.parking.domain;

/** Статус парковочного места (spots.status). */
public enum SpotStatus {
    AVAILABLE("Работает"),
    MAINTENANCE("На обслуживании"),
    CLOSED("Закрыто");

    private final String title;

    SpotStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    /** Можно ли оформлять на место новые заявки. */
    public boolean acceptsRequests() {
        return this == AVAILABLE;
    }
}

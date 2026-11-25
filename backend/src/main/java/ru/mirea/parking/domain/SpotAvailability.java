package ru.mirea.parking.domain;

/** Доступность места сейчас: вычисляется по статусу и текущим заявкам, в базе не хранится. */
public enum SpotAvailability {
    FREE_NOW("Свободно"),
    BUSY_NOW("Занято"),
    UNAVAILABLE("Недоступно");

    private final String title;

    SpotAvailability(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}

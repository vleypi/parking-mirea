package ru.mirea.parking.domain;

/** Активность автомобиля: вычисляется по числу заявок за последние 30 дней, в базе не хранится. */
public enum VehicleActivity {
    LOW("Низкая"),
    MEDIUM("Средняя"),
    HIGH("Высокая");

    private final String title;

    VehicleActivity(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}

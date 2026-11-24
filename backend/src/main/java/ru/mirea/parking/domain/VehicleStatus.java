package ru.mirea.parking.domain;

/** Статус автомобиля (vehicles.status). */
public enum VehicleStatus {
    ACTIVE("Допущен"),
    BLOCKED("Въезд запрещён"),
    DEREGISTERED("Снят с учёта");

    private final String title;

    VehicleStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}

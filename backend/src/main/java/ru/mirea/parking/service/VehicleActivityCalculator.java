package ru.mirea.parking.service;

import ru.mirea.parking.domain.VehicleActivity;

import java.time.LocalDateTime;

/**
 * Показатель «Активность» автомобиля по числу его заявок за последние 30 дней без отменённых:
 * 0 — LOW, 1–3 — MEDIUM, 4 и больше — HIGH. То же правило для фильтра — VehicleSpecification.hasActivity.
 */
public final class VehicleActivityCalculator {

    public static final int WINDOW_DAYS = 30;
    public static final long MEDIUM_FROM = 1;
    public static final long HIGH_FROM = 4;

    private VehicleActivityCalculator() {
    }

    public static VehicleActivity calculate(long requestsInWindow) {
        if (requestsInWindow < 0) {
            throw new IllegalArgumentException("Количество заявок не может быть отрицательным: " + requestsInWindow);
        }
        if (requestsInWindow >= HIGH_FROM) {
            return VehicleActivity.HIGH;
        }
        if (requestsInWindow >= MEDIUM_FROM) {
            return VehicleActivity.MEDIUM;
        }
        return VehicleActivity.LOW;
    }

    /** Начало окна: заявки с началом не раньше этого момента (будущие брони тоже считаются). */
    public static LocalDateTime windowStart(LocalDateTime now) {
        return now.minusDays(WINDOW_DAYS);
    }
}

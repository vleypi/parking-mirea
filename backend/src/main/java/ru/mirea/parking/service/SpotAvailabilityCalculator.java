package ru.mirea.parking.service;

import ru.mirea.parking.domain.SpotAvailability;
import ru.mirea.parking.domain.SpotStatus;

import java.time.LocalDateTime;

/**
 * Показатель «Доступность» места: UNAVAILABLE — статус MAINTENANCE или CLOSED; BUSY_NOW — есть новая или подтверждённая
 * заявка, период которой включает текущий момент; иначе FREE_NOW. То же правило для фильтра — SpotSpecification.hasAvailability.
 */
public final class SpotAvailabilityCalculator {

    private SpotAvailabilityCalculator() {
    }

    public static SpotAvailability calculate(SpotStatus status, boolean occupiedNow) {
        if (!status.acceptsRequests()) {
            return SpotAvailability.UNAVAILABLE;
        }
        return occupiedNow ? SpotAvailability.BUSY_NOW : SpotAvailability.FREE_NOW;
    }

    /** isOccupiedNow из КР2: начало включается, конец нет — в момент окончания место уже свободно. */
    public static boolean covers(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        return !start.isAfter(now) && end.isAfter(now);
    }
}

package ru.mirea.project.dto;

import java.math.BigDecimal;

import ru.mirea.project.model.entity.ParkingSpot;
import ru.mirea.project.model.value.Period;

public record ParkingSpotStatistics(
    ParkingSpot spot,
    Period period,
    long requestCount,
    BigDecimal occupiedHours,
    BigDecimal revenue
) {
}

package ru.mirea.project.dto;

import java.math.BigDecimal;

import ru.mirea.project.model.value.Period;

public record GeneralStatistics(
    Period period,
    long totalUsers,
    long totalVehicles,
    long totalSpots,
    long requests,
    long active,
    long completed,
    long cancelled,
    long occupiedNow,
    BigDecimal revenue
) {
}

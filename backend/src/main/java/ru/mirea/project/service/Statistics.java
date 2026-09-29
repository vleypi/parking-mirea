package ru.mirea.project.service;

import java.math.BigDecimal;

public record Statistics(
    long totalUsers,
    long totalVehicles,
    long totalSpots,
    long totalRequests,
    long active,
    long completed,
    long cancelled,
    long occupiedNow,
    BigDecimal completedRevenue
) {
}

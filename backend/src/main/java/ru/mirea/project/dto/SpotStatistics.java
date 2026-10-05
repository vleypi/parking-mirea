package ru.mirea.project.dto;

import java.math.BigDecimal;

import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.value.Period;

public record SpotStatistics(
    Spot spot,
    Period period,
    long requestCount,
    BigDecimal occupiedHours,
    BigDecimal revenue
) {
}

package ru.mirea.project.dto.statistics;

import java.math.BigDecimal;
import java.util.Map;

import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.model.enums.RequestStatus;
import ru.mirea.project.model.value.Period;

public record VehicleStatistics(
    Vehicle vehicle,
    Period period,
    Map<RequestStatus, Long> requestsByStatus,
    BigDecimal parkedHours,
    BigDecimal totalPaid
) {
}

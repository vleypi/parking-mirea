package ru.mirea.project.dto.statistics;

import java.math.BigDecimal;
import java.util.Map;

import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.enums.RequestStatus;
import ru.mirea.project.model.value.Period;

public record UserStatistics(
    User user,
    Period period,
    long vehicleCount,
    Map<RequestStatus, Long> requestsByStatus,
    BigDecimal totalPaid
) {
}

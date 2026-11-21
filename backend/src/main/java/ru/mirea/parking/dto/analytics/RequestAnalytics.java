package ru.mirea.parking.dto.analytics;

import ru.mirea.parking.domain.RequestPriority;

import java.math.BigDecimal;
import java.util.Map;

public record RequestAnalytics(
        long total,
        long active,
        long archived,
        long paid,
        long unpaid,
        BigDecimal averageCost,
        Map<String, Long> byStatus,
        Map<String, Long> bySpotType,
        Map<RequestPriority, Long> byPriority) {
}

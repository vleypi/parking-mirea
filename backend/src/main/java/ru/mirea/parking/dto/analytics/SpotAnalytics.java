package ru.mirea.parking.dto.analytics;

import ru.mirea.parking.domain.SpotAvailability;
import ru.mirea.parking.domain.SpotStatus;

import java.math.BigDecimal;
import java.util.Map;

/** Блок «Парковочные места» общей сводки. byType — по названию типа из справочника. */
public record SpotAnalytics(
        long total,
        long active,
        long archived,
        long covered,
        BigDecimal averageHourlyRate,
        Map<String, Long> byType,
        Map<SpotStatus, Long> byStatus,
        Map<SpotAvailability, Long> byAvailability) {
}

package ru.mirea.parking.dto.analytics;

import ru.mirea.parking.domain.VehicleActivity;
import ru.mirea.parking.domain.VehicleStatus;

import java.math.BigDecimal;
import java.util.Map;

/** Блок «Автомобили» общей сводки. averageReleaseYear — средний год по машинам, где он указан. */
public record VehicleAnalytics(
        long total,
        long active,
        long archived,
        long electric,
        BigDecimal averageReleaseYear,
        Map<String, Long> byBrand,
        Map<VehicleStatus, Long> byStatus,
        Map<VehicleActivity, Long> byActivity) {
}

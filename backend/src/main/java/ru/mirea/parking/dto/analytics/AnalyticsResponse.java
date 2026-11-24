package ru.mirea.parking.dto.analytics;

/** Общая аналитическая сводка: по блоку на каждую сущность. */
public record AnalyticsResponse(RequestAnalytics requests, OwnerAnalytics owners, VehicleAnalytics vehicles,
                                SpotAnalytics spots) {
}

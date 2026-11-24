package ru.mirea.parking.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.parking.dto.analytics.AnalyticsResponse;
import ru.mirea.parking.dto.analytics.OwnerAnalytics;
import ru.mirea.parking.dto.analytics.VehicleAnalytics;
import ru.mirea.parking.filter.OwnerFilter;
import ru.mirea.parking.filter.RequestFilter;
import ru.mirea.parking.filter.VehicleFilter;
import ru.mirea.parking.service.OwnerAnalyticsService;
import ru.mirea.parking.service.RequestAnalyticsService;
import ru.mirea.parking.service.VehicleAnalyticsService;

@Tag(name = "analytics")
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final RequestAnalyticsService requestAnalytics;
    private final OwnerAnalyticsService ownerAnalytics;
    private final VehicleAnalyticsService vehicleAnalytics;

    public AnalyticsController(RequestAnalyticsService requestAnalytics, OwnerAnalyticsService ownerAnalytics,
                               VehicleAnalyticsService vehicleAnalytics) {
        this.requestAnalytics = requestAnalytics;
        this.ownerAnalytics = ownerAnalytics;
        this.vehicleAnalytics = vehicleAnalytics;
    }

    /**
     * Общая сводка. Параметры запроса относятся к блоку заявок; блок владельцев считается по всем владельцам
     * (активным и архивным). Блок владельцев с фильтрами списка — GET /api/analytics/owners.
     */
    @GetMapping("/summary")
    public AnalyticsResponse summary(@ModelAttribute RequestFilter requestFilter) {
        return new AnalyticsResponse(requestAnalytics.summarize(requestFilter), ownerAnalytics.summarize(OwnerFilter.empty()),
                vehicleAnalytics.summarize(VehicleFilter.empty()));
    }

    /** Аналитика владельцев с теми же параметрами, что у GET /api/owners. */
    @GetMapping("/owners")
    public OwnerAnalytics owners(@ModelAttribute OwnerFilter ownerFilter) {
        return ownerAnalytics.summarize(ownerFilter);
    }

    /** Аналитика автомобилей с теми же параметрами, что у GET /api/vehicles. */
    @GetMapping("/vehicles")
    public VehicleAnalytics vehicles(@ModelAttribute VehicleFilter vehicleFilter) {
        return vehicleAnalytics.summarize(vehicleFilter);
    }
}

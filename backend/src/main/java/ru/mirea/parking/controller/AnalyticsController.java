package ru.mirea.parking.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.parking.dto.analytics.AnalyticsResponse;
import ru.mirea.parking.dto.analytics.OwnerAnalytics;
import ru.mirea.parking.filter.OwnerFilter;
import ru.mirea.parking.filter.RequestFilter;
import ru.mirea.parking.service.OwnerAnalyticsService;
import ru.mirea.parking.service.RequestAnalyticsService;

@Tag(name = "analytics")
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final RequestAnalyticsService requestAnalytics;
    private final OwnerAnalyticsService ownerAnalytics;

    public AnalyticsController(RequestAnalyticsService requestAnalytics, OwnerAnalyticsService ownerAnalytics) {
        this.requestAnalytics = requestAnalytics;
        this.ownerAnalytics = ownerAnalytics;
    }

    /**
     * Общая сводка. Параметры запроса относятся к блоку заявок; блок владельцев считается по всем владельцам
     * (активным и архивным). Блок владельцев с фильтрами списка — GET /api/analytics/owners.
     */
    @GetMapping("/summary")
    public AnalyticsResponse summary(@ModelAttribute RequestFilter requestFilter) {
        return new AnalyticsResponse(requestAnalytics.summarize(requestFilter), ownerAnalytics.summarize(OwnerFilter.empty()));
    }

    /** Аналитика владельцев с теми же параметрами, что у GET /api/owners. */
    @GetMapping("/owners")
    public OwnerAnalytics owners(@ModelAttribute OwnerFilter ownerFilter) {
        return ownerAnalytics.summarize(ownerFilter);
    }
}

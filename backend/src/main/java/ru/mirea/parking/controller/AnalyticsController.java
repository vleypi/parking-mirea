package ru.mirea.parking.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.parking.dto.analytics.AnalyticsResponse;
import ru.mirea.parking.filter.RequestFilter;
import ru.mirea.parking.service.RequestAnalyticsService;

@Tag(name = "analytics")
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final RequestAnalyticsService requestAnalytics;

    public AnalyticsController(RequestAnalyticsService requestAnalytics) {
        this.requestAnalytics = requestAnalytics;
    }

    @GetMapping("/summary")
    public AnalyticsResponse summary(@ModelAttribute RequestFilter requestFilter) {
        return new AnalyticsResponse(requestAnalytics.summarize(requestFilter));
    }
}

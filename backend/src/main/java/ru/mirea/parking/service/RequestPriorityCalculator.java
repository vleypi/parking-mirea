package ru.mirea.parking.service;

import org.springframework.stereotype.Component;
import ru.mirea.parking.domain.Request;
import ru.mirea.parking.domain.RequestPriority;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import static ru.mirea.parking.domain.RequestStatus.CONFIRMED;
import static ru.mirea.parking.domain.RequestStatus.NEW;

@Component
public class RequestPriorityCalculator {

    public static final Duration HIGH_WINDOW = Duration.ofHours(24);
    public static final Duration MEDIUM_WINDOW = Duration.ofDays(3);

    private final Clock clock;

    public RequestPriorityCalculator(Clock clock) {
        this.clock = clock;
    }

    public RequestPriority calculate(Request request) {
        return calculate(request.getStatus().getCode(), request.getStartTime(), LocalDateTime.now(clock));
    }

    public static RequestPriority calculate(String statusCode, LocalDateTime startTime, LocalDateTime now) {
        boolean isNew = NEW.equals(statusCode);
        boolean isActive = isNew || CONFIRMED.equals(statusCode);

        if (isNew && startTime.isBefore(now.plus(HIGH_WINDOW))) {
            return RequestPriority.HIGH;
        }
        if (isActive && startTime.isBefore(now.plus(MEDIUM_WINDOW))) {
            return RequestPriority.MEDIUM;
        }
        return RequestPriority.LOW;
    }
}

package ru.mirea.parking.filter;

import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.parking.domain.Request;
import ru.mirea.parking.domain.RequestPriority;

import java.time.LocalDateTime;
import java.util.List;

import static ru.mirea.parking.domain.RequestStatus.CONFIRMED;
import static ru.mirea.parking.domain.RequestStatus.NEW;
import static ru.mirea.parking.service.RequestPriorityCalculator.HIGH_WINDOW;
import static ru.mirea.parking.service.RequestPriorityCalculator.MEDIUM_WINDOW;

public final class RequestSpecifications {

    private RequestSpecifications() {
    }

    public static Specification<Request> hasPriority(RequestPriority priority, LocalDateTime now) {
        LocalDateTime highBorder = now.plus(HIGH_WINDOW);
        LocalDateTime mediumBorder = now.plus(MEDIUM_WINDOW);

        return (root, query, cb) -> {
            Path<String> status = root.get("status").get("code");
            Path<LocalDateTime> start = root.get("startTime");

            var high = cb.and(cb.equal(status, NEW), cb.lessThan(start, highBorder));
            var medium = cb.or(
                    cb.and(cb.equal(status, CONFIRMED), cb.lessThan(start, mediumBorder)),
                    cb.and(cb.equal(status, NEW),
                            cb.greaterThanOrEqualTo(start, highBorder),
                            cb.lessThan(start, mediumBorder)));
            var low = cb.or(
                    cb.not(status.in(List.of(NEW, CONFIRMED))),
                    cb.greaterThanOrEqualTo(start, mediumBorder));

            return switch (priority) {
                case HIGH -> high;
                case MEDIUM -> medium;
                case LOW -> low;
            };
        };
    }
}

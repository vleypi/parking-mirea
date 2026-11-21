package ru.mirea.parking.filter;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.parking.domain.Request;
import ru.mirea.parking.domain.RequestPriority;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static ru.mirea.parking.domain.RequestStatus.CONFIRMED;
import static ru.mirea.parking.domain.RequestStatus.NEW;
import static ru.mirea.parking.service.RequestPriorityCalculator.HIGH_WINDOW;
import static ru.mirea.parking.service.RequestPriorityCalculator.MEDIUM_WINDOW;

public final class RequestSpecifications {

    private static final char ESCAPE = '\\';

    private RequestSpecifications() {
    }

    public static Specification<Request> byFilter(RequestFilter filter, LocalDateTime now) {
        List<Specification<Request>> specs = new ArrayList<>();
        specs.add(archived(filter.archived() == null ? ArchiveScope.ACTIVE : filter.archived()));
        if (hasText(filter.q())) {
            specs.add(textSearch(filter.q()));
        }
        if (hasText(filter.status())) {
            specs.add(hasStatus(filter.status()));
        }
        if (hasText(filter.spotType())) {
            specs.add(hasSpotType(filter.spotType()));
        }
        if (filter.paid() != null) {
            specs.add(isPaid(filter.paid()));
        }
        if (filter.priority() != null) {
            specs.add(hasPriority(filter.priority(), now));
        }
        if (hasText(filter.tag())) {
            specs.add(hasTag(filter.tag()));
        }
        if (filter.cost() != null) {
            specs.add(costMatches(filter.costOp() == null ? CompareOp.EQ : filter.costOp(), filter.cost()));
        } else if (filter.costOp() != null) {
            throw new ResponseStatusException(BAD_REQUEST, "Параметр costOp указан без cost");
        }
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new ResponseStatusException(BAD_REQUEST, "Параметр from должен быть не позже to");
        }
        if (filter.from() != null) {
            specs.add(startsFrom(filter.from()));
        }
        if (filter.to() != null) {
            specs.add(startsTo(filter.to()));
        }
        return Specification.allOf(specs);
    }

    public static Specification<Request> archived(ArchiveScope scope) {
        return (root, query, cb) -> switch (scope) {
            case ACTIVE -> cb.isFalse(root.get("archived"));
            case ARCHIVED -> cb.isTrue(root.get("archived"));
            case ALL -> cb.conjunction();
        };
    }

    public static Specification<Request> textSearch(String text) {
        String pattern = "%" + escapeLike(text.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), pattern, ESCAPE),
                cb.like(cb.lower(root.get("vehicle").get("licensePlate")), pattern, ESCAPE),
                cb.like(cb.lower(root.get("owner").get("name")), pattern, ESCAPE));
    }

    public static Specification<Request> hasStatus(String code) {
        return (root, query, cb) -> cb.equal(root.get("status").get("code"), code.trim().toUpperCase(Locale.ROOT));
    }

    public static Specification<Request> hasSpotType(String code) {
        return (root, query, cb) -> cb.equal(root.get("spot").get("spotType").get("code"),
                code.trim().toUpperCase(Locale.ROOT));
    }

    public static Specification<Request> isPaid(boolean paid) {
        return (root, query, cb) -> cb.equal(root.get("paid"), paid);
    }

    public static Specification<Request> hasTag(String tag) {
        String value = tag.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.isMember(value, root.get("tags"));
    }

    public static Specification<Request> costMatches(CompareOp op, BigDecimal value) {
        return (root, query, cb) -> {
            Expression<BigDecimal> cost = root.get("cost");
            return op.apply(cb, cost, value);
        };
    }

    public static Specification<Request> startsFrom(LocalDateTime from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startTime"), from);
    }

    public static Specification<Request> startsTo(LocalDateTime to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startTime"), to);
    }

    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
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

package ru.mirea.parking.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.parking.domain.Request;
import ru.mirea.parking.domain.RequestStatus;
import ru.mirea.parking.domain.Spot;
import ru.mirea.parking.domain.SpotAvailability;
import ru.mirea.parking.domain.SpotStatus;
import ru.mirea.parking.error.BadRequestException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Условия поиска парковочных мест через Criteria API, значения передаются параметрами, без конкатенации в SQL. */
public final class SpotSpecification {

    private static final List<String> ACTIVE_REQUESTS = List.of(RequestStatus.NEW, RequestStatus.CONFIRMED);

    private SpotSpecification() {
    }

    public static Specification<Spot> byFilter(SpotFilter filter, LocalDateTime now) {
        List<Specification<Spot>> specs = new ArrayList<>();
        specs.add(archived(filter.archived() == null ? ArchiveScope.ACTIVE : filter.archived()));
        if (hasText(filter.q())) {
            specs.add(textSearch(filter.q()));
        }
        if (filter.spotTypeId() != null) {
            specs.add(hasType(filter.spotTypeId()));
        }
        if (filter.status() != null) {
            specs.add(hasStatus(filter.status()));
        }
        if (filter.covered() != null) {
            specs.add(isCovered(filter.covered()));
        }
        if (filter.availability() != null) {
            specs.add(hasAvailability(filter.availability(), now));
        }
        if (hasText(filter.tag())) {
            specs.add(hasTag(filter.tag()));
        }
        if (filter.rate() != null) {
            specs.add(rateMatches(filter.rateOp() == null ? CompareOp.EQ : filter.rateOp(), filter.rate()));
        } else if (filter.rateOp() != null) {
            throw new BadRequestException("rateOp", "Параметр rateOp указан без rate");
        }
        return Specification.allOf(specs);
    }

    public static Specification<Spot> archived(ArchiveScope scope) {
        return (root, query, cb) -> switch (scope) {
            case ACTIVE -> cb.isFalse(root.get("archived"));
            case ARCHIVED -> cb.isTrue(root.get("archived"));
            case ALL -> cb.conjunction();
        };
    }

    /** Число — точный номер места; любой текст — фрагмент зоны или описания без учёта регистра. */
    public static Specification<Spot> textSearch(String text) {
        String words = LikePattern.contains(text);
        Integer number = parseNumber(text);
        return (root, query, cb) -> {
            Predicate zoneOrDescription = cb.or(
                    cb.like(cb.lower(root.get("zone")), words, LikePattern.ESCAPE),
                    cb.like(cb.lower(root.get("description")), words, LikePattern.ESCAPE));
            return number == null ? zoneOrDescription : cb.or(cb.equal(root.get("spotNumber"), number), zoneOrDescription);
        };
    }

    public static Specification<Spot> hasType(Short spotTypeId) {
        return (root, query, cb) -> cb.equal(root.get("spotType").get("id"), spotTypeId);
    }

    public static Specification<Spot> hasStatus(SpotStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Spot> isCovered(boolean covered) {
        return (root, query, cb) -> cb.equal(root.get("covered"), covered);
    }

    public static Specification<Spot> hasTag(String tag) {
        String value = tag.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.isMember(value, root.get("tags"));
    }

    public static Specification<Spot> rateMatches(CompareOp op, BigDecimal rate) {
        return (root, query, cb) -> op.apply(cb, root.get("hourlyRate"), rate);
    }

    /**
     * Доступность как условие запроса — то же правило, что в SpotAvailabilityCalculator:
     * UNAVAILABLE — статус не AVAILABLE; BUSY_NOW — AVAILABLE и EXISTS текущей заявки; FREE_NOW — AVAILABLE и NOT EXISTS.
     */
    public static Specification<Spot> hasAvailability(SpotAvailability availability, LocalDateTime now) {
        return (root, query, cb) -> {
            Predicate works = cb.equal(root.get("status"), SpotStatus.AVAILABLE);
            return switch (availability) {
                case UNAVAILABLE -> cb.not(works);
                case BUSY_NOW -> cb.and(works, cb.exists(activeRequests(root, query, cb, now, now)));
                case FREE_NOW -> cb.and(works, cb.not(cb.exists(activeRequests(root, query, cb, now, now))));
            };
        };
    }

    /** findFreeDuring из КР2: работающее место без новой или подтверждённой заявки, пересекающей период [from, to). */
    public static Specification<Spot> freeDuring(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), SpotStatus.AVAILABLE),
                cb.not(cb.exists(activeRequests(root, query, cb, from, to))));
    }

    /**
     * Подзапрос «есть новая или подтверждённая заявка места, пересекающая [from, to)»: start &lt; to и end &gt; from.
     * При from = to = now это ровно условие «заявка идёт сейчас»: start &lt;= now и end &gt; now.
     */
    private static Subquery<Long> activeRequests(Root<Spot> root, CriteriaQuery<?> query, CriteriaBuilder cb,
                                                 LocalDateTime from, LocalDateTime to) {
        Subquery<Long> sub = query.subquery(Long.class);
        Root<Request> request = sub.from(Request.class);
        Predicate startsBefore = from.equals(to)
                ? cb.lessThanOrEqualTo(request.get("startTime"), to)
                : cb.lessThan(request.get("startTime"), to);
        sub.select(request.get("id")).where(
                cb.equal(request.get("spot"), root),
                request.get("status").get("code").in(ACTIVE_REQUESTS),
                startsBefore,
                cb.greaterThan(request.get("endTime"), from));
        return sub;
    }

    private static Integer parseNumber(String text) {
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

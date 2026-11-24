package ru.mirea.parking.filter;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.parking.domain.LicensePlates;
import ru.mirea.parking.domain.Request;
import ru.mirea.parking.domain.RequestStatus;
import ru.mirea.parking.domain.Vehicle;
import ru.mirea.parking.domain.VehicleActivity;
import ru.mirea.parking.domain.VehicleStatus;
import ru.mirea.parking.error.BadRequestException;
import ru.mirea.parking.service.VehicleActivityCalculator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static ru.mirea.parking.service.VehicleActivityCalculator.HIGH_FROM;
import static ru.mirea.parking.service.VehicleActivityCalculator.MEDIUM_FROM;

/** Условия поиска автомобилей через Criteria API, значения передаются параметрами, без конкатенации в SQL. */
public final class VehicleSpecification {

    private VehicleSpecification() {
    }

    public static Specification<Vehicle> byFilter(VehicleFilter filter, LocalDateTime now) {
        List<Specification<Vehicle>> specs = new ArrayList<>();
        specs.add(archived(filter.archived() == null ? ArchiveScope.ACTIVE : filter.archived()));
        if (hasText(filter.q())) {
            specs.add(textSearch(filter.q()));
        }
        if (filter.ownerId() != null) {
            specs.add(hasOwner(filter.ownerId()));
        }
        if (hasText(filter.region())) {
            specs.add(hasRegion(filter.region().trim()));
        }
        if (filter.status() != null) {
            specs.add(hasStatus(filter.status()));
        }
        if (filter.electric() != null) {
            specs.add(isElectric(filter.electric()));
        }
        if (filter.activity() != null) {
            specs.add(hasActivity(filter.activity(), now));
        }
        if (hasText(filter.tag())) {
            specs.add(hasTag(filter.tag()));
        }
        if (filter.year() != null) {
            specs.add(yearMatches(filter.yearOp() == null ? CompareOp.EQ : filter.yearOp(), filter.year()));
        } else if (filter.yearOp() != null) {
            throw new BadRequestException("yearOp", "Параметр yearOp указан без year");
        }
        return Specification.allOf(specs);
    }

    public static Specification<Vehicle> archived(ArchiveScope scope) {
        return (root, query, cb) -> switch (scope) {
            case ACTIVE -> cb.isFalse(root.get("archived"));
            case ARCHIVED -> cb.isTrue(root.get("archived"));
            case ALL -> cb.conjunction();
        };
    }

    /** Фрагмент гос. номера (латиница приводится к кириллице, как в КР2), марка или модель. */
    public static Specification<Vehicle> textSearch(String text) {
        String plate = "%" + LikePattern.escape(LicensePlates.normalizeText(text)) + "%";
        String words = LikePattern.contains(text);
        return (root, query, cb) -> cb.or(
                cb.like(root.get("licensePlate"), plate, LikePattern.ESCAPE),
                cb.like(cb.lower(root.get("brand")), words, LikePattern.ESCAPE),
                cb.like(cb.lower(root.get("model")), words, LikePattern.ESCAPE));
    }

    public static Specification<Vehicle> hasOwner(UUID ownerId) {
        return (root, query, cb) -> cb.equal(root.get("owner").get("uuid"), ownerId);
    }

    public static Specification<Vehicle> hasRegion(String region) {
        return (root, query, cb) -> cb.equal(root.get("region"), region);
    }

    public static Specification<Vehicle> hasStatus(VehicleStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Vehicle> isElectric(boolean electric) {
        return (root, query, cb) -> cb.equal(root.get("electric"), electric);
    }

    public static Specification<Vehicle> hasTag(String tag) {
        String value = tag.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.isMember(value, root.get("tags"));
    }

    public static Specification<Vehicle> yearMatches(CompareOp op, Short year) {
        return (root, query, cb) -> op.apply(cb, root.get("releaseYear"), year);
    }

    /**
     * Активность как условие запроса — то же правило, что в VehicleActivityCalculator:
     * (select count(r) from Request r where r.vehicle = v and r.startTime >= :since and r.status.code <> 'CANCELLED').
     */
    public static Specification<Vehicle> hasActivity(VehicleActivity activity, LocalDateTime now) {
        LocalDateTime since = VehicleActivityCalculator.windowStart(now);
        return (root, query, cb) -> {
            Subquery<Long> recent = query.subquery(Long.class);
            Root<Request> request = recent.from(Request.class);
            recent.select(cb.count(request)).where(
                    cb.equal(request.get("vehicle"), root),
                    cb.greaterThanOrEqualTo(request.get("startTime"), since),
                    cb.notEqual(request.get("status").get("code"), RequestStatus.CANCELLED));
            return switch (activity) {
                case LOW -> cb.lessThan(recent, MEDIUM_FROM);
                case MEDIUM -> cb.and(cb.greaterThanOrEqualTo(recent, MEDIUM_FROM), cb.lessThan(recent, HIGH_FROM));
                case HIGH -> cb.greaterThanOrEqualTo(recent, HIGH_FROM);
            };
        };
    }

    /** Есть неотменённая заявка, пересекающая период [from, to) — логика findParkedDuring из КР2. */
    public static Specification<Vehicle> parkedDuring(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            Subquery<Long> overlapping = query.subquery(Long.class);
            Root<Request> request = overlapping.from(Request.class);
            overlapping.select(request.get("id")).where(
                    cb.equal(request.get("vehicle"), root),
                    cb.notEqual(request.get("status").get("code"), RequestStatus.CANCELLED),
                    cb.lessThan(request.get("startTime"), to),
                    cb.greaterThan(request.get("endTime"), from));
            return cb.exists(overlapping);
        };
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

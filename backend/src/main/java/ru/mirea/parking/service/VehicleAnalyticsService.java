package ru.mirea.parking.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.parking.domain.Vehicle;
import ru.mirea.parking.domain.VehicleActivity;
import ru.mirea.parking.domain.VehicleStatus;
import ru.mirea.parking.dto.analytics.VehicleAnalytics;
import ru.mirea.parking.filter.ArchiveScope;
import ru.mirea.parking.filter.VehicleFilter;
import ru.mirea.parking.filter.VehicleSpecification;
import ru.mirea.parking.repository.VehicleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Аналитика автомобилей агрегирующими запросами (COUNT, SUM, AVG, GROUP BY) с условиями списка. */
@Service
public class VehicleAnalyticsService {

    private final EntityManager em;
    private final VehicleRepository vehicles;
    private final Clock clock;

    public VehicleAnalyticsService(EntityManager em, VehicleRepository vehicles, Clock clock) {
        this.em = em;
        this.vehicles = vehicles;
        this.clock = clock;
    }

    /** Если archived не указан, считаются и активные, и архивные машины. */
    @Transactional(readOnly = true)
    public VehicleAnalytics summarize(VehicleFilter filter) {
        VehicleFilter effective = filter.archived() == null ? filter.withArchived(ArchiveScope.ALL) : filter;
        LocalDateTime now = LocalDateTime.now(clock);
        Specification<Vehicle> spec = VehicleSpecification.byFilter(effective, now);

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Vehicle> root = query.from(Vehicle.class);
        query.multiselect(
                cb.count(root),
                cb.sum(countIf(cb, cb.isTrue(root.get("archived")))),
                cb.sum(countIf(cb, cb.isTrue(root.get("electric")))),
                cb.avg(root.get("releaseYear")));
        query.where(spec.toPredicate(root, query, cb));
        Tuple totals = em.createQuery(query).getSingleResult();

        long total = totals.get(0, Long.class);
        long archived = orZero(totals.get(1, Long.class));
        long electric = orZero(totals.get(2, Long.class));
        Double avgYear = totals.get(3, Double.class);
        BigDecimal averageYear = avgYear == null ? null : BigDecimal.valueOf(avgYear).setScale(1, RoundingMode.HALF_UP);

        Map<String, Long> byBrand = new LinkedHashMap<>();
        groupBy(spec, r -> r.get("brand")).entrySet().stream()
                .sorted(Map.Entry.<Object, Long>comparingByValue().reversed())
                .forEach(e -> byBrand.put((String) e.getKey(), e.getValue()));

        Map<VehicleStatus, Long> byStatus = new EnumMap<>(VehicleStatus.class);
        for (VehicleStatus status : VehicleStatus.values()) {
            byStatus.put(status, 0L);
        }
        groupBy(spec, r -> r.get("status")).forEach((status, count) -> byStatus.put((VehicleStatus) status, count));

        // Активность не хранится: на каждый уровень — COUNT с тем же подзапросом, что у фильтра.
        Map<VehicleActivity, Long> byActivity = new EnumMap<>(VehicleActivity.class);
        for (VehicleActivity level : VehicleActivity.values()) {
            byActivity.put(level, vehicles.count(spec.and(VehicleSpecification.hasActivity(level, now))));
        }

        return new VehicleAnalytics(total, total - archived, archived, electric, averageYear, byBrand, byStatus, byActivity);
    }

    private Map<Object, Long> groupBy(Specification<Vehicle> spec, Function<Root<Vehicle>, Path<?>> key) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Vehicle> root = query.from(Vehicle.class);
        Path<?> path = key.apply(root);
        query.multiselect(path, cb.count(root));
        query.where(spec.toPredicate(root, query, cb));
        query.groupBy(path);

        Map<Object, Long> result = new LinkedHashMap<>();
        for (Tuple row : em.createQuery(query).getResultList()) {
            result.put(row.get(0), row.get(1, Long.class));
        }
        return result;
    }

    private static Expression<Long> countIf(CriteriaBuilder cb, Expression<Boolean> condition) {
        return cb.<Long>selectCase().when(condition, 1L).otherwise(0L);
    }

    private static long orZero(Long value) {
        return value == null ? 0 : value;
    }
}

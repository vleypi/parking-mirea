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
import ru.mirea.parking.domain.Spot;
import ru.mirea.parking.domain.SpotAvailability;
import ru.mirea.parking.domain.SpotStatus;
import ru.mirea.parking.dto.analytics.SpotAnalytics;
import ru.mirea.parking.filter.ArchiveScope;
import ru.mirea.parking.filter.SpotFilter;
import ru.mirea.parking.filter.SpotSpecification;
import ru.mirea.parking.repository.SpotRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/** Аналитика парковочных мест агрегирующими запросами (COUNT, SUM, AVG, GROUP BY) с условиями списка. */
@Service
public class SpotAnalyticsService {

    private final EntityManager em;
    private final SpotRepository spots;
    private final Clock clock;

    public SpotAnalyticsService(EntityManager em, SpotRepository spots, Clock clock) {
        this.em = em;
        this.spots = spots;
        this.clock = clock;
    }

    /** Если archived не указан, считаются и активные, и архивные места. */
    @Transactional(readOnly = true)
    public SpotAnalytics summarize(SpotFilter filter) {
        SpotFilter effective = filter.archived() == null ? filter.withArchived(ArchiveScope.ALL) : filter;
        LocalDateTime now = LocalDateTime.now(clock);
        Specification<Spot> spec = SpotSpecification.byFilter(effective, now);

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Spot> root = query.from(Spot.class);
        query.multiselect(
                cb.count(root),
                cb.sum(countIf(cb, cb.isTrue(root.get("archived")))),
                cb.sum(countIf(cb, cb.isTrue(root.get("covered")))),
                cb.avg(root.get("hourlyRate")));
        query.where(spec.toPredicate(root, query, cb));
        Tuple totals = em.createQuery(query).getSingleResult();

        long total = totals.get(0, Long.class);
        long archived = orZero(totals.get(1, Long.class));
        long covered = orZero(totals.get(2, Long.class));
        Double avgRate = totals.get(3, Double.class);
        BigDecimal averageRate = avgRate == null ? null : BigDecimal.valueOf(avgRate).setScale(2, RoundingMode.HALF_UP);

        Map<String, Long> byType = new LinkedHashMap<>();
        groupBy(spec, r -> r.get("spotType").get("title")).forEach((title, count) -> byType.put((String) title, count));

        Map<SpotStatus, Long> byStatus = new EnumMap<>(SpotStatus.class);
        for (SpotStatus status : SpotStatus.values()) {
            byStatus.put(status, 0L);
        }
        groupBy(spec, r -> r.get("status")).forEach((status, count) -> byStatus.put((SpotStatus) status, count));

        // Доступность не хранится: на каждое значение — COUNT с тем же условием (EXISTS), что у фильтра.
        Map<SpotAvailability, Long> byAvailability = new EnumMap<>(SpotAvailability.class);
        for (SpotAvailability value : SpotAvailability.values()) {
            byAvailability.put(value, spots.count(spec.and(SpotSpecification.hasAvailability(value, now))));
        }

        return new SpotAnalytics(total, total - archived, archived, covered, averageRate, byType, byStatus, byAvailability);
    }

    private Map<Object, Long> groupBy(Specification<Spot> spec, Function<Root<Spot>, Path<?>> key) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Spot> root = query.from(Spot.class);
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

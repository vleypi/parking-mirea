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
import ru.mirea.parking.domain.Request;
import ru.mirea.parking.domain.RequestPriority;
import ru.mirea.parking.domain.RequestStatus;
import ru.mirea.parking.domain.SpotType;
import ru.mirea.parking.dto.analytics.RequestAnalytics;
import ru.mirea.parking.filter.ArchiveScope;
import ru.mirea.parking.filter.RequestFilter;
import ru.mirea.parking.filter.RequestSpecifications;
import ru.mirea.parking.repository.RequestRepository;
import ru.mirea.parking.repository.RequestStatusRepository;
import ru.mirea.parking.repository.SpotTypeRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class RequestAnalyticsService {

    private final EntityManager em;
    private final RequestRepository requests;
    private final RequestStatusRepository statuses;
    private final SpotTypeRepository spotTypes;
    private final Clock clock;

    public RequestAnalyticsService(EntityManager em, RequestRepository requests, RequestStatusRepository statuses,
                                   SpotTypeRepository spotTypes, Clock clock) {
        this.em = em;
        this.requests = requests;
        this.statuses = statuses;
        this.spotTypes = spotTypes;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RequestAnalytics summarize(RequestFilter filter) {
        RequestFilter effective = filter.archived() == null ? filter.withArchived(ArchiveScope.ALL) : filter;
        LocalDateTime now = LocalDateTime.now(clock);
        Specification<Request> spec = RequestSpecifications.byFilter(effective, now);

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Request> root = query.from(Request.class);
        query.multiselect(
                cb.count(root),
                cb.sum(countIf(cb, cb.isTrue(root.get("archived")))),
                cb.sum(countIf(cb, cb.isTrue(root.get("paid")))),
                cb.avg(root.get("cost")));
        query.where(spec.toPredicate(root, query, cb));
        Tuple totals = em.createQuery(query).getSingleResult();

        long total = totals.get(0, Long.class);
        long archived = orZero(totals.get(1, Long.class));
        long paid = orZero(totals.get(2, Long.class));
        Double avg = totals.get(3, Double.class);
        BigDecimal averageCost = avg == null
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);

        Map<String, Long> byStatus = zeros(statuses.findAll().stream()
                .sorted(Comparator.comparing(RequestStatus::getId)).map(RequestStatus::getCode).toList());
        byStatus.putAll(groupBy(spec, root2 -> root2.get("status").get("code")));

        Map<String, Long> bySpotType = zeros(spotTypes.findAll().stream()
                .sorted(Comparator.comparing(SpotType::getId)).map(SpotType::getCode).toList());
        bySpotType.putAll(groupBy(spec, root2 -> root2.get("spot").get("spotType").get("code")));

        Map<RequestPriority, Long> byPriority = new EnumMap<>(RequestPriority.class);
        for (RequestPriority priority : RequestPriority.values()) {
            byPriority.put(priority, requests.count(spec.and(RequestSpecifications.hasPriority(priority, now))));
        }

        return new RequestAnalytics(total, total - archived, archived, paid, total - paid, averageCost,
                byStatus, bySpotType, byPriority);
    }

    private Map<String, Long> groupBy(Specification<Request> spec, Function<Root<Request>, Path<String>> key) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Request> root = query.from(Request.class);
        Path<String> path = key.apply(root);
        query.multiselect(path, cb.count(root));
        query.where(spec.toPredicate(root, query, cb));
        query.groupBy(path);

        Map<String, Long> result = new LinkedHashMap<>();
        for (Tuple row : em.createQuery(query).getResultList()) {
            result.put(row.get(0, String.class), row.get(1, Long.class));
        }
        return result;
    }

    private static Expression<Long> countIf(CriteriaBuilder cb, Expression<Boolean> condition) {
        return cb.<Long>selectCase().when(condition, 1L).otherwise(0L);
    }

    private static Map<String, Long> zeros(Iterable<String> keys) {
        Map<String, Long> map = new LinkedHashMap<>();
        keys.forEach(k -> map.put(k, 0L));
        return map;
    }

    private static long orZero(Long value) {
        return value == null ? 0 : value;
    }
}

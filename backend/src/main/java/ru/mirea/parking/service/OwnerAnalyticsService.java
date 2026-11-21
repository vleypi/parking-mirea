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
import ru.mirea.parking.domain.ClientCategory;
import ru.mirea.parking.domain.LoyaltyLevel;
import ru.mirea.parking.domain.Owner;
import ru.mirea.parking.domain.OwnerStatus;
import ru.mirea.parking.dto.analytics.OwnerAnalytics;
import ru.mirea.parking.filter.ArchiveScope;
import ru.mirea.parking.filter.OwnerFilter;
import ru.mirea.parking.filter.OwnerSpecifications;
import ru.mirea.parking.repository.ClientCategoryRepository;
import ru.mirea.parking.repository.OwnerRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Аналитика по владельцам. Всё считается агрегирующими запросами в базе (COUNT, SUM, AVG, GROUP BY),
 * а не перебором записей в Java. Условия те же, что у списка GET /api/owners (OwnerSpecifications),
 * поэтому цифры сходятся со списком. Если archived не указан, считаются и активные, и архивные.
 */
@Service
public class OwnerAnalyticsService {

    private final EntityManager em;
    private final OwnerRepository owners;
    private final ClientCategoryRepository categories;

    public OwnerAnalyticsService(EntityManager em, OwnerRepository owners, ClientCategoryRepository categories) {
        this.em = em;
        this.owners = owners;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public OwnerAnalytics summarize(OwnerFilter filter) {
        OwnerFilter effective = filter.archived() == null ? filter.withArchived(ArchiveScope.ALL) : filter;
        Specification<Owner> spec = OwnerSpecifications.byFilter(effective);

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Owner> root = query.from(Owner.class);
        query.multiselect(
                cb.count(root),
                cb.sum(countIf(cb, cb.isTrue(root.get("archived")))),
                cb.sum(countIf(cb, cb.isTrue(root.get("notificationsEnabled")))),
                cb.avg(root.get("discountPercent")));
        query.where(spec.toPredicate(root, query, cb));
        Tuple totals = em.createQuery(query).getSingleResult();

        long total = totals.get(0, Long.class);
        long archived = orZero(totals.get(1, Long.class));
        long notifications = orZero(totals.get(2, Long.class));
        Double avg = totals.get(3, Double.class);
        BigDecimal averageDiscount = avg == null
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);

        Map<String, Long> byCategory = new LinkedHashMap<>();
        categories.findAllByOrderByIdAsc().stream().map(ClientCategory::getCode).forEach(code -> byCategory.put(code, 0L));
        groupBy(spec, r -> r.get("category").get("code")).forEach((code, count) -> byCategory.put((String) code, count));

        Map<OwnerStatus, Long> byStatus = new EnumMap<>(OwnerStatus.class);
        for (OwnerStatus status : OwnerStatus.values()) {
            byStatus.put(status, 0L);
        }
        groupBy(spec, r -> r.get("status")).forEach((status, count) -> byStatus.put((OwnerStatus) status, count));

        // Лояльность не хранится в таблице, поэтому группировка — это COUNT с подзапросом на каждый из трёх уровней.
        Map<LoyaltyLevel, Long> byLoyalty = new EnumMap<>(LoyaltyLevel.class);
        for (LoyaltyLevel level : LoyaltyLevel.values()) {
            byLoyalty.put(level, owners.count(spec.and(OwnerSpecifications.hasLoyalty(level))));
        }

        return new OwnerAnalytics(total, total - archived, archived, notifications, averageDiscount,
                byCategory, byStatus, byLoyalty);
    }

    private Map<Object, Long> groupBy(Specification<Owner> spec, java.util.function.Function<Root<Owner>, Path<?>> key) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Owner> root = query.from(Owner.class);
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

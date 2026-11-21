package ru.mirea.parking.filter;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import ru.mirea.parking.domain.LoyaltyLevel;
import ru.mirea.parking.domain.Owner;
import ru.mirea.parking.domain.Request;

import static ru.mirea.parking.domain.RequestStatus.COMPLETED;
import static ru.mirea.parking.service.LoyaltyCalculator.REGULAR_FROM;
import static ru.mirea.parking.service.LoyaltyCalculator.VIP_FROM;

/** Условия поиска владельцев (Criteria API, без конкатенации строк в SQL/JPQL). */
public final class OwnerSpecifications {

    private OwnerSpecifications() {
    }

    /**
     * Уровень лояльности как условие запроса. Подзапрос считает завершённые заявки владельца:
     * <pre>
     * (select count(r) from Request r where r.owner = o and r.status.code = 'COMPLETED')
     * </pre>
     * NEW: count &lt; 3, REGULAR: 3 &le; count &lt; 10, VIP: count &ge; 10 — те же границы, что в LoyaltyCalculator.
     */
    public static Specification<Owner> hasLoyalty(LoyaltyLevel level) {
        return (root, query, cb) -> {
            Subquery<Long> completed = query.subquery(Long.class);
            Root<Request> request = completed.from(Request.class);
            completed.select(cb.count(request)).where(
                    cb.equal(request.get("owner"), root),
                    cb.equal(request.get("status").get("code"), COMPLETED));

            return switch (level) {
                case NEW -> cb.lessThan(completed, REGULAR_FROM);
                case REGULAR -> cb.and(cb.greaterThanOrEqualTo(completed, REGULAR_FROM), cb.lessThan(completed, VIP_FROM));
                case VIP -> cb.greaterThanOrEqualTo(completed, VIP_FROM);
            };
        };
    }
}

package ru.mirea.parking.filter;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.parking.domain.LoyaltyLevel;
import ru.mirea.parking.domain.Owner;
import ru.mirea.parking.domain.OwnerStatus;
import ru.mirea.parking.domain.Request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static ru.mirea.parking.domain.RequestStatus.COMPLETED;
import static ru.mirea.parking.service.LoyaltyCalculator.REGULAR_FROM;
import static ru.mirea.parking.service.LoyaltyCalculator.VIP_FROM;

/** Условия поиска владельцев (Criteria API, без конкатенации строк в SQL/JPQL). */
public final class OwnerSpecifications {

    private static final Pattern PHONE_QUERY = Pattern.compile("[\\d\\s+()-]*\\d[\\d\\s+()-]*");

    private OwnerSpecifications() {
    }

    public static Specification<Owner> byFilter(OwnerFilter filter) {
        List<Specification<Owner>> specs = new ArrayList<>();
        specs.add(archived(filter.archived() == null ? ArchiveScope.ACTIVE : filter.archived()));
        if (hasText(filter.q())) {
            specs.add(textSearch(filter.q()));
        }
        if (filter.categoryId() != null) {
            specs.add(hasCategory(filter.categoryId()));
        }
        if (filter.status() != null) {
            specs.add(hasStatus(filter.status()));
        }
        if (filter.notifications() != null) {
            specs.add(notificationsEnabled(filter.notifications()));
        }
        if (filter.loyalty() != null) {
            specs.add(hasLoyalty(filter.loyalty()));
        }
        if (hasText(filter.tag())) {
            specs.add(hasTag(filter.tag()));
        }
        if (filter.discount() != null) {
            specs.add(discountMatches(filter.discountOp() == null ? CompareOp.EQ : filter.discountOp(), filter.discount()));
        } else if (filter.discountOp() != null) {
            throw new ResponseStatusException(BAD_REQUEST, "Параметр discountOp указан без discount");
        }
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new ResponseStatusException(BAD_REQUEST, "Параметр from должен быть не позже to");
        }
        if (filter.from() != null) {
            specs.add(registeredFrom(filter.from()));
        }
        if (filter.to() != null) {
            specs.add(registeredTo(filter.to()));
        }
        return Specification.allOf(specs);
    }

    public static Specification<Owner> archived(ArchiveScope scope) {
        return (root, query, cb) -> switch (scope) {
            case ACTIVE -> cb.isFalse(root.get("archived"));
            case ARCHIVED -> cb.isTrue(root.get("archived"));
            case ALL -> cb.conjunction();
        };
    }

    /**
     * Имя, email или телефон содержат текст. Если запрос похож на номер (только цифры, пробелы, «+», «-», скобки),
     * его цифры ищутся ещё и в телефоне без «+» и «-»: запрос «900 100» найдёт +7-900-100-00-01.
     * Текст передаётся параметром LIKE с экранированием % и _, без конкатенации в SQL.
     */
    public static Specification<Owner> textSearch(String text) {
        String pattern = LikePattern.contains(text);
        String digits = PHONE_QUERY.matcher(text).matches() ? text.replaceAll("\\D", "") : "";
        return (root, query, cb) -> {
            List<Predicate> any = new ArrayList<>();
            any.add(cb.like(cb.lower(root.get("name")), pattern, LikePattern.ESCAPE));
            any.add(cb.like(cb.lower(root.get("email")), pattern, LikePattern.ESCAPE));
            any.add(cb.like(root.get("phone"), pattern, LikePattern.ESCAPE));
            if (!digits.isEmpty()) {
                Expression<String> phoneDigits = cb.function("replace", String.class,
                        cb.function("replace", String.class, root.get("phone"), cb.literal("-"), cb.literal("")),
                        cb.literal("+"), cb.literal(""));
                any.add(cb.like(phoneDigits, "%" + digits + "%"));
            }
            return cb.or(any.toArray(Predicate[]::new));
        };
    }

    public static Specification<Owner> hasCategory(Short categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Owner> hasStatus(OwnerStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Owner> notificationsEnabled(boolean enabled) {
        return (root, query, cb) -> cb.equal(root.get("notificationsEnabled"), enabled);
    }

    public static Specification<Owner> hasTag(String tag) {
        String value = tag.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.isMember(value, root.get("tags"));
    }

    public static Specification<Owner> discountMatches(CompareOp op, BigDecimal value) {
        return (root, query, cb) -> {
            Expression<BigDecimal> discount = root.get("discountPercent");
            return op.apply(cb, discount, value);
        };
    }

    public static Specification<Owner> registeredFrom(LocalDateTime from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<Owner> registeredTo(LocalDateTime to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
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

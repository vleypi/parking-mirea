package ru.mirea.parking.filter;

import org.springframework.format.annotation.DateTimeFormat;
import ru.mirea.parking.domain.LoyaltyLevel;
import ru.mirea.parking.domain.OwnerStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Параметры GET /api/owners. Все условия необязательные и складываются через AND.
 *
 * @param q             имя, цифры телефона или email (часть строки)
 * @param categoryId    категория клиента из справочника
 * @param status        ACTIVE / BLOCKED
 * @param notifications включены ли уведомления
 * @param loyalty       уровень лояльности (вычисляется подзапросом)
 * @param tag           тег
 * @param archived      ACTIVE (по умолчанию) / ARCHIVED / ALL
 * @param discount      скидка для сравнения
 * @param discountOp    eq, gt, gte, lt, lte (по умолчанию eq)
 * @param from          дата регистрации с
 * @param to            дата регистрации по
 */
public record OwnerFilter(
        String q,
        Short categoryId,
        OwnerStatus status,
        Boolean notifications,
        LoyaltyLevel loyalty,
        String tag,
        ArchiveScope archived,
        BigDecimal discount,
        CompareOp discountOp,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

    public static OwnerFilter empty() {
        return new OwnerFilter(null, null, null, null, null, null, null, null, null, null, null);
    }

    public OwnerFilter withArchived(ArchiveScope scope) {
        return new OwnerFilter(q, categoryId, status, notifications, loyalty, tag, scope, discount, discountOp, from, to);
    }
}

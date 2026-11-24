package ru.mirea.parking.filter;

import ru.mirea.parking.domain.SpotAvailability;
import ru.mirea.parking.domain.SpotStatus;

import java.math.BigDecimal;

/**
 * Параметры GET /api/spots, все необязательные и складываются через AND.
 * q — номер, зона или описание; rate + rateOp — сравнение тарифа (eq, gt, gte, lt, lte).
 */
public record SpotFilter(
        String q,
        Short spotTypeId,
        SpotStatus status,
        Boolean covered,
        SpotAvailability availability,
        String tag,
        ArchiveScope archived,
        BigDecimal rate,
        CompareOp rateOp) {

    public static SpotFilter empty() {
        return new SpotFilter(null, null, null, null, null, null, null, null, null);
    }

    public SpotFilter withArchived(ArchiveScope scope) {
        return new SpotFilter(q, spotTypeId, status, covered, availability, tag, scope, rate, rateOp);
    }
}

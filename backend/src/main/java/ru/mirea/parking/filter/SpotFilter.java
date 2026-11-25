package ru.mirea.parking.filter;

import org.springframework.format.annotation.DateTimeFormat;
import ru.mirea.parking.domain.SpotAvailability;
import ru.mirea.parking.domain.SpotStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Параметры GET /api/spots, все необязательные и складываются через AND.
 * q — номер, зона или описание; rate + rateOp — сравнение тарифа (eq, gt, gte, lt, lte).
 * freeFrom/freeTo — места, свободные на период (работают и не имеют активной заявки в периоде).
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
        CompareOp rateOp,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime freeFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime freeTo) {

    public static SpotFilter empty() {
        return new SpotFilter(null, null, null, null, null, null, null, null, null, null, null);
    }

    public SpotFilter withArchived(ArchiveScope scope) {
        return new SpotFilter(q, spotTypeId, status, covered, availability, tag, scope, rate, rateOp,
                freeFrom, freeTo);
    }
}

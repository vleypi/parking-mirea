package ru.mirea.parking.filter;

import org.springframework.format.annotation.DateTimeFormat;
import ru.mirea.parking.domain.RequestPriority;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RequestFilter(
        String q,
        String status,
        String spotType,
        Boolean paid,
        RequestPriority priority,
        String tag,
        ArchiveScope archived,
        BigDecimal cost,
        CompareOp costOp,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

    public RequestFilter withArchived(ArchiveScope scope) {
        return new RequestFilter(q, status, spotType, paid, priority, tag, scope, cost, costOp, from, to);
    }
}

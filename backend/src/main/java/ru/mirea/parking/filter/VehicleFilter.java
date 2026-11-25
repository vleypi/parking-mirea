package ru.mirea.parking.filter;

import org.springframework.format.annotation.DateTimeFormat;
import ru.mirea.parking.domain.VehicleActivity;
import ru.mirea.parking.domain.VehicleStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Параметры GET /api/vehicles, все необязательные и складываются через AND.
 * q — фрагмент гос. номера, марка или модель; year + yearOp — сравнение года выпуска (eq, gt, gte, lt, lte).
 * parkedFrom/parkedTo — машины, стоявшие на парковке в период (неотменённые заявки, пересекающие период).
 */
public record VehicleFilter(
        String q,
        UUID ownerId,
        String region,
        VehicleStatus status,
        Boolean electric,
        VehicleActivity activity,
        String tag,
        ArchiveScope archived,
        Short year,
        CompareOp yearOp,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime parkedFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime parkedTo) {

    public static VehicleFilter empty() {
        return new VehicleFilter(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public VehicleFilter withArchived(ArchiveScope scope) {
        return new VehicleFilter(q, ownerId, region, status, electric, activity, tag, scope, year, yearOp,
                parkedFrom, parkedTo);
    }
}

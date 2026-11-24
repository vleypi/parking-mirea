package ru.mirea.parking.filter;

import ru.mirea.parking.domain.VehicleActivity;
import ru.mirea.parking.domain.VehicleStatus;

import java.util.UUID;

/**
 * Параметры GET /api/vehicles, все необязательные и складываются через AND.
 * q — фрагмент гос. номера, марка или модель; year + yearOp — сравнение года выпуска (eq, gt, gte, lt, lte).
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
        CompareOp yearOp) {

    public static VehicleFilter empty() {
        return new VehicleFilter(null, null, null, null, null, null, null, null, null, null);
    }

    public VehicleFilter withArchived(ArchiveScope scope) {
        return new VehicleFilter(q, ownerId, region, status, electric, activity, tag, scope, year, yearOp);
    }
}

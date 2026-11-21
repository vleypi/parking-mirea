package ru.mirea.parking.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mirea.parking.domain.Request;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, Long> {

    @EntityGraph(attributePaths = {"owner", "vehicle", "spot", "status", "tags"})
    Optional<Request> findByUuid(UUID uuid);

    @Query("""
            select count(r) > 0 from Request r
            where r.spot.id = :spotId
              and r.id <> :excludeId
              and r.archived = false
              and r.status.code in :activeStatuses
              and r.startTime < :end
              and r.endTime > :start
            """)
    boolean existsSpotOverlap(@Param("spotId") Long spotId, @Param("excludeId") Long excludeId,
                              @Param("activeStatuses") Collection<String> activeStatuses,
                              @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            select count(r) > 0 from Request r
            where r.vehicle.id = :vehicleId
              and r.id <> :excludeId
              and r.archived = false
              and r.status.code in :activeStatuses
              and r.startTime < :end
              and r.endTime > :start
            """)
    boolean existsVehicleOverlap(@Param("vehicleId") Long vehicleId, @Param("excludeId") Long excludeId,
                                 @Param("activeStatuses") Collection<String> activeStatuses,
                                 @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

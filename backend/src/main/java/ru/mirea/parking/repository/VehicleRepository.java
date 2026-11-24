package ru.mirea.parking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mirea.parking.domain.Request;
import ru.mirea.parking.domain.Vehicle;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {

    @EntityGraph(attributePaths = {"owner", "tags"})
    Optional<Vehicle> findByUuid(UUID uuid);

    @EntityGraph(attributePaths = {"owner", "tags"})
    Optional<Vehicle> findByLicensePlate(String licensePlate);

    List<Vehicle> findByOwnerIdOrderByLicensePlateAsc(Long ownerId);

    boolean existsByLicensePlate(String licensePlate);

    boolean existsByLicensePlateAndIdNot(String licensePlate, Long id);

    @Override
    @EntityGraph(attributePaths = {"owner"})
    Page<Vehicle> findAll(Specification<Vehicle> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"owner"})
    List<Vehicle> findAll(Specification<Vehicle> spec, Sort sort);

    @Query("select count(r) > 0 from Request r where r.vehicle.id = :vehicleId")
    boolean hasRequests(@Param("vehicleId") Long vehicleId);

    /** Заявки за окно активности (без отменённых) по каждой машине страницы одним GROUP BY. */
    @Query("""
            select r.vehicle.id, count(r) from Request r
            where r.vehicle.id in :vehicleIds and r.startTime >= :since and r.status.code <> 'CANCELLED'
            group by r.vehicle.id
            """)
    List<Object[]> countRecentRequests(@Param("vehicleIds") Collection<Long> vehicleIds,
                                       @Param("since") LocalDateTime since);

    /** Последние заявки машины для карточки, вместе с архивными. */
    @Query("""
            select r from Request r join fetch r.owner join fetch r.spot join fetch r.status
            where r.vehicle.id = :vehicleId
            order by r.startTime desc, r.id desc
            """)
    List<Request> findRequests(@Param("vehicleId") Long vehicleId, Pageable pageable);

    @Query("select count(r) from Request r where r.vehicle.id = :vehicleId")
    long countRequests(@Param("vehicleId") Long vehicleId);
}

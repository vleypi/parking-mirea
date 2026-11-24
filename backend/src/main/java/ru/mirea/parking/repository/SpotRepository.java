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
import ru.mirea.parking.domain.Spot;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpotRepository extends JpaRepository<Spot, Long>, JpaSpecificationExecutor<Spot> {

    @EntityGraph(attributePaths = {"spotType", "tags"})
    Optional<Spot> findByUuid(UUID uuid);

    @EntityGraph(attributePaths = {"spotType", "tags"})
    Optional<Spot> findBySpotNumber(Integer spotNumber);

    boolean existsBySpotNumber(Integer spotNumber);

    boolean existsBySpotNumberAndIdNot(Integer spotNumber, Long id);

    @Override
    @EntityGraph(attributePaths = {"spotType"})
    Page<Spot> findAll(Specification<Spot> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"spotType"})
    List<Spot> findAll(Specification<Spot> spec, Sort sort);

    @Query("select count(r) > 0 from Request r where r.spot.id = :spotId")
    boolean hasRequests(@Param("spotId") Long spotId);

    /** Новые и подтверждённые заявки, которые ещё не закончились: с ними место в архив не переводится. */
    @Query("""
            select count(r) > 0 from Request r
            where r.spot.id = :spotId and r.status.code in ('NEW', 'CONFIRMED') and r.endTime > :now
            """)
    boolean hasActiveRequestsAfter(@Param("spotId") Long spotId, @Param("now") LocalDateTime now);

    /** Места страницы, занятые прямо сейчас (правило SpotAvailabilityCalculator.covers), одним запросом. */
    @Query("""
            select distinct r.spot.id from Request r
            where r.spot.id in :spotIds and r.status.code in ('NEW', 'CONFIRMED')
              and r.startTime <= :now and r.endTime > :now
            """)
    List<Long> findOccupiedAt(@Param("spotIds") Collection<Long> spotIds, @Param("now") LocalDateTime now);

    /** Последние заявки места для карточки, вместе с архивными. */
    @Query("""
            select r from Request r join fetch r.owner join fetch r.vehicle join fetch r.status
            where r.spot.id = :spotId
            order by r.startTime desc, r.id desc
            """)
    List<Request> findRequests(@Param("spotId") Long spotId, Pageable pageable);

    @Query("select count(r) from Request r where r.spot.id = :spotId")
    long countRequests(@Param("spotId") Long spotId);
}

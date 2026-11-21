package ru.mirea.parking.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mirea.parking.domain.Owner;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OwnerRepository extends JpaRepository<Owner, Long>, JpaSpecificationExecutor<Owner> {

    @EntityGraph(attributePaths = {"category", "tags"})
    Optional<Owner> findByUuid(UUID uuid);

    Optional<Owner> findByPhone(String phone);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    @Override
    @EntityGraph(attributePaths = {"category"})
    Page<Owner> findAll(Specification<Owner> spec, Pageable pageable);

    @Query("select count(v) > 0 from Vehicle v where v.owner.id = :ownerId")
    boolean hasVehicles(@Param("ownerId") Long ownerId);

    @Query("select count(r) > 0 from Request r where r.owner.id = :ownerId")
    boolean hasRequests(@Param("ownerId") Long ownerId);

    /** Количество машин у каждого владельца из списка одним запросом (без N+1 на странице). */
    @Query("""
            select v.owner.id, count(v) from Vehicle v
            where v.owner.id in :ownerIds
            group by v.owner.id
            """)
    List<Object[]> countVehiclesByOwnerIds(@Param("ownerIds") Collection<Long> ownerIds);

    /** Количество завершённых заявок у каждого владельца из списка — основа уровня лояльности. */
    @Query("""
            select r.owner.id, count(r) from Request r
            where r.owner.id in :ownerIds and r.status.code = :status
            group by r.owner.id
            """)
    List<Object[]> countRequestsByOwnerIds(@Param("ownerIds") Collection<Long> ownerIds, @Param("status") String status);
}

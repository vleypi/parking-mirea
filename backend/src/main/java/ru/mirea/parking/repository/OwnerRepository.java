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

    /** Количество машин у каждого владельца из списка одним запросом (без N+1 на странице). */
    @Query("""
            select v.owner.id, count(v) from Vehicle v
            where v.owner.id in :ownerIds
            group by v.owner.id
            """)
    List<Object[]> countVehiclesByOwnerIds(@Param("ownerIds") Collection<Long> ownerIds);
}

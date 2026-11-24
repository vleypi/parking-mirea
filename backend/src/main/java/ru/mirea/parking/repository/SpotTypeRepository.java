package ru.mirea.parking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.mirea.parking.domain.SpotType;

import java.util.List;
import java.util.Optional;

public interface SpotTypeRepository extends JpaRepository<SpotType, Short> {

    List<SpotType> findAllByOrderBySortOrderAscIdAsc();

    Optional<SpotType> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Short id);

    boolean existsByTitleIgnoreCaseAndIdNot(String title, Short id);

    /** Следующий свободный id: у справочника id задаётся явно, без последовательности. */
    @Query("select coalesce(max(t.id), 0) + 1 from SpotType t")
    int nextId();
}

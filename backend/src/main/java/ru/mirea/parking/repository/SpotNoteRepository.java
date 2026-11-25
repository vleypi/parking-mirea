package ru.mirea.parking.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.parking.domain.SpotNote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpotNoteRepository extends JpaRepository<SpotNote, UUID> {

    /** Закреплённые первыми, дальше от новых к старым. */
    @EntityGraph(attributePaths = "author")
    List<SpotNote> findBySpotIdOrderByPinnedDescCreatedAtDesc(Long spotId);

    @EntityGraph(attributePaths = "author")
    Optional<SpotNote> findByIdAndSpotId(UUID id, Long spotId);
}

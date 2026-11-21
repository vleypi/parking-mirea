package ru.mirea.parking.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.parking.domain.OwnerNote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OwnerNoteRepository extends JpaRepository<OwnerNote, UUID> {

    /** Закреплённые заметки первыми, дальше от новых к старым. */
    @EntityGraph(attributePaths = "author")
    List<OwnerNote> findByOwnerIdOrderByPinnedDescCreatedAtDesc(Long ownerId);

    @EntityGraph(attributePaths = "author")
    Optional<OwnerNote> findByIdAndOwnerId(UUID id, Long ownerId);
}

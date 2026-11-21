package ru.mirea.parking.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.parking.domain.RequestNote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequestNoteRepository extends JpaRepository<RequestNote, UUID> {

    @EntityGraph(attributePaths = "author")
    List<RequestNote> findByRequestIdOrderByPinnedDescCreatedAtDesc(Long requestId);

    @EntityGraph(attributePaths = "author")
    Optional<RequestNote> findByIdAndRequestId(UUID id, Long requestId);
}

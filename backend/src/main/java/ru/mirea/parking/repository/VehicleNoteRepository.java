package ru.mirea.parking.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.parking.domain.VehicleNote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleNoteRepository extends JpaRepository<VehicleNote, UUID> {

    /** Закреплённые первыми, дальше от новых к старым. */
    @EntityGraph(attributePaths = "author")
    List<VehicleNote> findByVehicleIdOrderByPinnedDescCreatedAtDesc(Long vehicleId);

    @EntityGraph(attributePaths = "author")
    Optional<VehicleNote> findByIdAndVehicleId(UUID id, Long vehicleId);
}

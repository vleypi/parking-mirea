package ru.mirea.parking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.parking.domain.Spot;

import java.util.Optional;
import java.util.UUID;

public interface SpotRepository extends JpaRepository<Spot, Long> {

    Optional<Spot> findByUuid(UUID uuid);
}

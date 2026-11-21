package ru.mirea.parking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.parking.domain.Owner;

import java.util.Optional;
import java.util.UUID;

public interface OwnerRepository extends JpaRepository<Owner, Long> {

    Optional<Owner> findByUuid(UUID uuid);
}

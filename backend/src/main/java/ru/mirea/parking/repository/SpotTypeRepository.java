package ru.mirea.parking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.parking.domain.SpotType;

public interface SpotTypeRepository extends JpaRepository<SpotType, Short> {
}

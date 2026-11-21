package ru.mirea.parking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.parking.domain.RequestStatus;

import java.util.Optional;

public interface RequestStatusRepository extends JpaRepository<RequestStatus, Short> {

    Optional<RequestStatus> findByCode(String code);
}

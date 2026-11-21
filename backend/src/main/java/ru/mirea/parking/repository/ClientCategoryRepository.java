package ru.mirea.parking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.parking.domain.ClientCategory;

import java.util.List;
import java.util.Optional;

public interface ClientCategoryRepository extends JpaRepository<ClientCategory, Short> {

    List<ClientCategory> findAllByOrderByIdAsc();

    Optional<ClientCategory> findByTitleIgnoreCase(String title);
}

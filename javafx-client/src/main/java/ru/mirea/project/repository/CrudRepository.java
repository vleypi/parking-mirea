package ru.mirea.project.repository;

import java.util.List;
import java.util.Optional;

public interface CrudRepository<T> {
    T create(T entity);

    List<T> findAll();

    Optional<T> findById(long id);

    void update(T entity);

    void delete(long id);
}

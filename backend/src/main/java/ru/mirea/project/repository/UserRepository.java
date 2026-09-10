package ru.mirea.project.repository;

import java.util.List;
import java.util.Optional;

import ru.mirea.project.model.User;

public class UserRepository implements CrudRepository<User> {

    @Override
    public User create(User user) {
        throw new UnsupportedOperationException("Не реализовано");
    }

    @Override
    public List<User> findAll() {
        throw new UnsupportedOperationException("Не реализовано");
    }

    @Override
    public Optional<User> findById(long id) {
        throw new UnsupportedOperationException("Не реализовано");
    }

    @Override
    public void update(User user) {
        throw new UnsupportedOperationException("Не реализовано");
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException("Не реализовано");
    }
}

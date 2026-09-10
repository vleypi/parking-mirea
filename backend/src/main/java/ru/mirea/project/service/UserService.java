package ru.mirea.project.service;

import java.util.List;

import ru.mirea.project.model.User;
import ru.mirea.project.repository.UserRepository;

public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User create(String name, String phone) {
        throw new UnsupportedOperationException("Не реализовано");
    }

    public List<User> getAll() {
        throw new UnsupportedOperationException("Не реализовано");
    }

    public User getById(long id) {
        throw new UnsupportedOperationException("Не реализовано");
    }

    public User update(long id, String name, String phone) {
        throw new UnsupportedOperationException("Не реализовано");
    }

    public void delete(long id) {
        throw new UnsupportedOperationException("Не реализовано");
    }
}

package ru.mirea.project.service;

import java.time.LocalDateTime;
import java.util.List;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.User;
import ru.mirea.project.repository.UserRepository;

public class UserService {
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_PHONE_LENGTH = 20;

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User create(String name, String phone) {
        validateNameAndPhone(name, phone);
        String trimmedName = name.trim();
        String trimmedPhone = phone.trim();
        checkPhoneUnique(0, trimmedPhone);

        User user = new User(0, trimmedName, trimmedPhone, LocalDateTime.now());
        return userRepository.create(user);
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public User getById(long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Владелец с id " + id + " не найден"));
    }

    public User update(long id, String name, String phone) {
        validateNameAndPhone(name, phone);
        String trimmedName = name.trim();
        String trimmedPhone = phone.trim();

        User existing = getById(id);
        checkPhoneUnique(id, trimmedPhone);

        existing.setName(trimmedName);
        existing.setPhone(trimmedPhone);
        userRepository.update(existing);
        return existing;
    }

    public void delete(long id) {
        getById(id);
        userRepository.delete(id);
    }

    private void validateNameAndPhone(String name, String phone) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Имя владельца обязательно для заполнения");
        }
        if (phone == null || phone.isBlank()) {
            throw new BusinessException("Телефон владельца обязателен для заполнения");
        }
        if (name.trim().length() > MAX_NAME_LENGTH) {
            throw new BusinessException("Имя владельца не должно быть длиннее " + MAX_NAME_LENGTH + " символов");
        }
        if (phone.trim().length() > MAX_PHONE_LENGTH) {
            throw new BusinessException("Телефон не должен быть длиннее " + MAX_PHONE_LENGTH + " символов");
        }
    }

    private void checkPhoneUnique(long excludeId, String phone) {
        boolean taken = userRepository.findAll().stream()
            .filter(u -> u.getId() != excludeId)
            .anyMatch(u -> u.getPhone().equals(phone));

        if (taken) {
            throw new BusinessException("Владелец с телефоном " + phone + " уже существует");
        }
    }
}

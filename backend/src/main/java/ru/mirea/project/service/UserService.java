package ru.mirea.project.service;

import java.time.LocalDateTime;
import java.util.List;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.User;
import ru.mirea.project.repository.UserRepository;

public class UserService {
    private static final int MAX_NAME_LENGTH = 100;

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User create(String name, String phone) {
        String normalizedName = checkName(name);
        String normalizedPhone = checkPhone(0, phone);

        User user = new User(0, normalizedName, normalizedPhone, LocalDateTime.now());
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
        User existing = getById(id);
        String normalizedName = checkName(name);
        String normalizedPhone = checkPhone(id, phone);

        existing.setName(normalizedName);
        existing.setPhone(normalizedPhone);
        userRepository.update(existing);
        return existing;
    }

    public void delete(long id) {
        getById(id);
        userRepository.delete(id);
    }

    public String checkName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Имя владельца обязательно для заполнения");
        }
        String normalized = InputFormats.normalizeName(name);
        if (normalized.length() > MAX_NAME_LENGTH) {
            throw new BusinessException("Имя владельца не должно быть длиннее " + MAX_NAME_LENGTH + " символов");
        }
        return normalized;
    }

    public String checkPhone(long ownerId, String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BusinessException("Телефон владельца обязателен для заполнения");
        }
        String normalized = InputFormats.normalizePhone(phone);
        checkPhoneUnique(ownerId, normalized);
        return normalized;
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

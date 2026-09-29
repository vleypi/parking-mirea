package ru.mirea.project.service;

import java.util.List;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.Vehicle;
import ru.mirea.project.repository.VehicleRepository;

public class VehicleService {
    private static final int MAX_BRAND_LENGTH = 50;
    private static final int MAX_MODEL_LENGTH = 50;

    private final VehicleRepository vehicleRepository;
    private final UserService userService;

    public VehicleService(VehicleRepository vehicleRepository, UserService userService) {
        this.vehicleRepository = vehicleRepository;
        this.userService = userService;
    }

    public Vehicle create(long userId, String licensePlate, String brand, String model) {
        userService.getById(userId);
        String plate = checkLicensePlate(0, licensePlate);
        String normalizedBrand = checkBrand(brand);
        String normalizedModel = checkModel(model);

        Vehicle vehicle = new Vehicle(0, userId, plate, normalizedBrand, normalizedModel);
        return vehicleRepository.create(vehicle);
    }

    public List<Vehicle> getAll() {
        return vehicleRepository.findAll();
    }

    public Vehicle getById(long id) {
        return vehicleRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Автомобиль с id " + id + " не найден"));
    }

    public List<Vehicle> getByUserId(long userId) {
        return vehicleRepository.findAll().stream()
            .filter(v -> v.getUserId() == userId)
            .toList();
    }

    public List<Vehicle> searchByLicensePlate(String fragment) {
        String needle = fragment == null ? "" : InputFormats.normalizePlateText(fragment);
        if (needle.isEmpty()) {
            throw new BusinessException("Введите фрагмент гос. номера для поиска");
        }
        return vehicleRepository.findAll().stream()
            .filter(v -> v.getLicensePlate().contains(needle))
            .toList();
    }

    public Vehicle update(long id, String licensePlate, String brand, String model) {
        Vehicle existing = getById(id);
        String plate = checkLicensePlate(id, licensePlate);
        String normalizedBrand = checkBrand(brand);
        String normalizedModel = checkModel(model);

        existing.setLicensePlate(plate);
        existing.setBrand(normalizedBrand);
        existing.setModel(normalizedModel);
        vehicleRepository.update(existing);
        return existing;
    }

    public void delete(long id) {
        getById(id);
        vehicleRepository.delete(id);
    }

    public String checkLicensePlate(long vehicleId, String licensePlate) {
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new BusinessException("Гос. номер обязателен для заполнения");
        }
        String normalized = InputFormats.normalizePlate(licensePlate);
        checkLicensePlateUnique(vehicleId, normalized);
        return normalized;
    }

    public String checkBrand(String brand) {
        return checkText(brand, "Марка", MAX_BRAND_LENGTH);
    }

    public String checkModel(String model) {
        return checkText(model, "Модель", MAX_MODEL_LENGTH);
    }

    private String checkText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(fieldName + " автомобиля обязательна для заполнения");
        }
        String normalized = InputFormats.normalizeTitle(value);
        if (normalized.length() > maxLength) {
            throw new BusinessException(fieldName + " автомобиля не должна быть длиннее " + maxLength + " символов");
        }
        return normalized;
    }

    private void checkLicensePlateUnique(long excludeId, String plate) {
        boolean taken = vehicleRepository.findAll().stream()
            .filter(v -> v.getId() != excludeId)
            .anyMatch(v -> v.getLicensePlate().equals(plate));

        if (taken) {
            throw new BusinessException("Автомобиль с гос. номером " + plate + " уже существует");
        }
    }
}

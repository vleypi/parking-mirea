package ru.mirea.project.service;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ru.mirea.project.dto.VehicleStatistics;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.entity.ParkingRequest;
import ru.mirea.project.model.entity.ParkingSpot;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.repository.ParkingRequestRepository;
import ru.mirea.project.repository.ParkingSpotRepository;
import ru.mirea.project.repository.VehicleRepository;
import ru.mirea.project.util.InputFormats;

public class VehicleService {
    private static final int MAX_BRAND_LENGTH = 50;
    private static final int MAX_MODEL_LENGTH = 50;

    private final VehicleRepository vehicleRepository;
    private final UserService userService;
    private final ParkingRequestRepository parkingRequestRepository;
    private final ParkingSpotRepository parkingSpotRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserService userService,
                          ParkingRequestRepository parkingRequestRepository, ParkingSpotRepository parkingSpotRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userService = userService;
        this.parkingRequestRepository = parkingRequestRepository;
        this.parkingSpotRepository = parkingSpotRepository;
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
        userService.getById(userId);
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

    public List<Vehicle> searchByBrandOrModel(String fragment) {
        String needle = InputFormats.requireText(fragment, "Введите фрагмент марки или модели для поиска").toLowerCase();
        return vehicleRepository.findAll().stream()
            .filter(v -> v.getBrand().toLowerCase().contains(needle) || v.getModel().toLowerCase().contains(needle))
            .toList();
    }

    public List<Vehicle> findParkedDuring(Period period) {
        if (!period.isBounded()) {
            throw new BusinessException("Укажите начало и конец периода");
        }
        Set<Long> vehicleIds = parkingRequestRepository.findAll().stream()
            .filter(r -> !r.isCancelled())
            .filter(r -> r.overlaps(period))
            .map(ParkingRequest::getVehicleId)
            .collect(Collectors.toSet());
        return vehicleRepository.findAll().stream()
            .filter(v -> vehicleIds.contains(v.getId()))
            .toList();
    }

    public List<Vehicle> filterByBrand(String brand) {
        String wanted = InputFormats.requireText(brand, "Введите марку");
        return vehicleRepository.findAll().stream()
            .filter(v -> v.getBrand().equalsIgnoreCase(wanted))
            .toList();
    }

    public List<Vehicle> sortByLicensePlate(boolean ascending) {
        return sorted(Comparator.comparing(Vehicle::getLicensePlate), ascending);
    }

    public List<Vehicle> sortByBrand(boolean ascending) {
        return sorted(Comparator.comparing(Vehicle::getBrand, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Vehicle::getModel, String.CASE_INSENSITIVE_ORDER), ascending);
    }

    public VehicleStatistics getStatistics(long vehicleId, Period period) {
        Vehicle vehicle = getById(vehicleId);
        List<ParkingRequest> requests = parkingRequestRepository.findAll().stream()
            .filter(r -> r.getVehicleId() == vehicleId)
            .filter(r -> r.overlaps(period))
            .toList();
        long minutes = requests.stream()
            .filter(r -> !r.isCancelled())
            .mapToLong(ParkingRequest::durationMinutes)
            .sum();
        return new VehicleStatistics(vehicle, period,
            ParkingRequest.countByStatus(requests),
            ParkingRequest.hours(minutes),
            ParkingRequest.completedRevenue(requests, ParkingSpot.ratesById(parkingSpotRepository.findAll())));
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

    private List<Vehicle> sorted(Comparator<Vehicle> comparator, boolean ascending) {
        return vehicleRepository.findAll().stream()
            .sorted(ascending ? comparator : comparator.reversed())
            .toList();
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

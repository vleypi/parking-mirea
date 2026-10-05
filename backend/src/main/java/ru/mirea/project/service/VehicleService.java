package ru.mirea.project.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.mirea.project.dto.filter.VehicleFilter;
import ru.mirea.project.dto.statistics.VehicleStatistics;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.entity.Request;
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.repository.RequestRepository;
import ru.mirea.project.repository.SpotRepository;
import ru.mirea.project.repository.VehicleRepository;
import ru.mirea.project.util.InputFormats;

public class VehicleService {
    private static final int MAX_BRAND_LENGTH = 50;
    private static final int MAX_MODEL_LENGTH = 50;

    private final VehicleRepository vehicleRepository;
    private final UserService userService;
    private final RequestRepository requestRepository;
    private final SpotRepository spotRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserService userService,
                          RequestRepository requestRepository, SpotRepository spotRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userService = userService;
        this.requestRepository = requestRepository;
        this.spotRepository = spotRepository;
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

    public List<Vehicle> findParkedDuring(Period period) {
        if (!period.isBounded()) {
            throw new BusinessException("Укажите начало и конец периода");
        }
        Set<Long> vehicleIds = requestRepository.findAll().stream()
            .filter(r -> !r.isCancelled())
            .filter(r -> r.overlaps(period))
            .map(Request::getVehicleId)
            .collect(Collectors.toSet());
        return vehicleRepository.findAll().stream()
            .filter(v -> vehicleIds.contains(v.getId()))
            .toList();
    }

    public List<Vehicle> find(VehicleFilter filter) {
        Map<Long, String> ownerNames = userService.getAll().stream()
            .collect(Collectors.toMap(User::getId, User::getName));

        Stream<Vehicle> vehicles = vehicleRepository.findAll().stream();
        if (filter.getPlateContains() != null) {
            String needle = filter.getPlateContains();
            vehicles = vehicles.filter(v -> v.getLicensePlate().contains(needle));
        }
        if (filter.getRegion() != null) {
            String region = filter.getRegion();
            vehicles = vehicles.filter(v -> v.getRegion().equals(region));
        }
        if (filter.getBrandContains() != null) {
            String needle = filter.getBrandContains().toLowerCase();
            vehicles = vehicles.filter(v -> v.getBrand().toLowerCase().contains(needle));
        }
        if (filter.getModelContains() != null) {
            String needle = filter.getModelContains().toLowerCase();
            vehicles = vehicles.filter(v -> v.getModel().toLowerCase().contains(needle));
        }
        if (filter.getOwnerNameContains() != null) {
            String needle = filter.getOwnerNameContains().toLowerCase();
            vehicles = vehicles.filter(v -> ownerNames.get(v.getUserId()).toLowerCase().contains(needle));
        }

        Comparator<Vehicle> comparator = switch (filter.getSortField()) {
            case ID -> Comparator.comparingLong(Vehicle::getId);
            case PLATE -> Comparator.comparing(Vehicle::getLicensePlate);
            case REGION -> Comparator.comparingInt((Vehicle v) -> Integer.parseInt(v.getRegion()));
            case BRAND -> Comparator.comparing(Vehicle::getBrand, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Vehicle::getModel, String.CASE_INSENSITIVE_ORDER);
            case MODEL -> Comparator.comparing(Vehicle::getModel, String.CASE_INSENSITIVE_ORDER);
            case OWNER -> Comparator.comparing((Vehicle v) -> ownerNames.get(v.getUserId()), String.CASE_INSENSITIVE_ORDER);
        };
        return vehicles.sorted(filter.isAscending() ? comparator : comparator.reversed()).toList();
    }

    public VehicleStatistics getStatistics(long vehicleId, Period period) {
        Vehicle vehicle = getById(vehicleId);
        List<Request> requests = requestRepository.findAll().stream()
            .filter(r -> r.getVehicleId() == vehicleId)
            .filter(r -> r.overlaps(period))
            .toList();
        long minutes = requests.stream()
            .filter(r -> !r.isCancelled())
            .mapToLong(Request::durationMinutes)
            .sum();
        return new VehicleStatistics(vehicle, period,
            Request.countByStatus(requests),
            Request.hours(minutes),
            Request.completedRevenue(requests, Spot.ratesById(spotRepository.findAll())));
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

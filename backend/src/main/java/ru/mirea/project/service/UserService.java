package ru.mirea.project.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ru.mirea.project.dto.UserStatistics;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.entity.ParkingRequest;
import ru.mirea.project.model.entity.ParkingSpot;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.repository.ParkingRequestRepository;
import ru.mirea.project.repository.ParkingSpotRepository;
import ru.mirea.project.repository.UserRepository;
import ru.mirea.project.repository.VehicleRepository;
import ru.mirea.project.util.InputFormats;

public class UserService {
    private static final int MAX_NAME_LENGTH = 100;

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingRequestRepository parkingRequestRepository;
    private final ParkingSpotRepository parkingSpotRepository;

    public UserService(UserRepository userRepository, VehicleRepository vehicleRepository,
                       ParkingRequestRepository parkingRequestRepository, ParkingSpotRepository parkingSpotRepository) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.parkingRequestRepository = parkingRequestRepository;
        this.parkingSpotRepository = parkingSpotRepository;
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

    public List<User> searchByName(String fragment) {
        String needle = InputFormats.requireText(fragment, "Введите фрагмент имени для поиска").toLowerCase();
        return userRepository.findAll().stream()
            .filter(u -> u.getName().toLowerCase().contains(needle))
            .toList();
    }

    public List<User> searchByPhone(String fragment) {
        String digits = fragment == null ? "" : fragment.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            throw new BusinessException("Введите цифры телефона для поиска");
        }
        return userRepository.findAll().stream()
            .filter(u -> u.getPhone().replaceAll("\\D", "").contains(digits))
            .toList();
    }

    public List<User> filterByHasVehicles(boolean hasVehicles) {
        Set<Long> ownerIds = vehicleRepository.findAll().stream()
            .map(Vehicle::getUserId)
            .collect(Collectors.toSet());
        return userRepository.findAll().stream()
            .filter(u -> ownerIds.contains(u.getId()) == hasVehicles)
            .toList();
    }

    public List<User> filterWithActiveRequests() {
        Set<Long> userIds = parkingRequestRepository.findAll().stream()
            .filter(ParkingRequest::isActive)
            .map(ParkingRequest::getUserId)
            .collect(Collectors.toSet());
        return userRepository.findAll().stream()
            .filter(u -> userIds.contains(u.getId()))
            .toList();
    }

    public List<User> sortByName(boolean ascending) {
        return sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER), ascending);
    }

    public List<User> sortByCreatedAt(boolean ascending) {
        return sorted(Comparator.comparing(User::getCreatedAt), ascending);
    }

    public UserStatistics getStatistics(long userId, Period period) {
        User user = getById(userId);
        long vehicleCount = vehicleRepository.findAll().stream()
            .filter(v -> v.getUserId() == userId)
            .count();
        List<ParkingRequest> requests = parkingRequestRepository.findAll().stream()
            .filter(r -> r.getUserId() == userId)
            .filter(r -> r.overlaps(period))
            .toList();
        return new UserStatistics(user, period, vehicleCount,
            ParkingRequest.countByStatus(requests),
            ParkingRequest.completedRevenue(requests, ParkingSpot.ratesById(parkingSpotRepository.findAll())));
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

    private List<User> sorted(Comparator<User> comparator, boolean ascending) {
        return userRepository.findAll().stream()
            .sorted(ascending ? comparator : comparator.reversed())
            .toList();
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

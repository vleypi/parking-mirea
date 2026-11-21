package ru.mirea.project.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.mirea.project.dto.filter.UserFilter;
import ru.mirea.project.dto.statistics.UserStatistics;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.entity.Request;
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.model.value.NumberRange;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.repository.RequestRepository;
import ru.mirea.project.repository.SpotRepository;
import ru.mirea.project.repository.UserRepository;
import ru.mirea.project.repository.VehicleRepository;
import ru.mirea.project.util.InputFormats;

public class UserService {
    private static final int MAX_NAME_LENGTH = 100;

    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final RequestRepository requestRepository;
    private final SpotRepository spotRepository;

    public UserService(UserRepository userRepository, VehicleRepository vehicleRepository,
                       RequestRepository requestRepository, SpotRepository spotRepository) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.requestRepository = requestRepository;
        this.spotRepository = spotRepository;
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

    public List<User> find(UserFilter filter) {
        Map<Long, Long> vehicleCounts = vehicleRepository.findAll().stream()
            .collect(Collectors.groupingBy(Vehicle::getUserId, Collectors.counting()));
        Set<Long> usersWithActiveRequests = requestRepository.findAll().stream()
            .filter(Request::isActive)
            .map(Request::getUserId)
            .collect(Collectors.toSet());

        Stream<User> users = userRepository.findAll().stream();
        if (filter.getNameContains() != null) {
            String needle = filter.getNameContains().toLowerCase();
            users = users.filter(u -> u.getName().toLowerCase().contains(needle));
        }
        if (filter.getPhoneDigits() != null) {
            String digits = filter.getPhoneDigits();
            users = users.filter(u -> u.getPhone().replaceAll("\\D", "").contains(digits));
        }
        if (filter.getVehicleCount() != null) {
            NumberRange range = filter.getVehicleCount();
            users = users.filter(u -> range.contains(vehicleCounts.getOrDefault(u.getId(), 0L)));
        }
        if (filter.getRegistered() != null) {
            Period period = filter.getRegistered();
            users = users.filter(u -> period.contains(u.getCreatedAt()));
        }
        if (filter.getHasActiveRequests() != null) {
            boolean hasActive = filter.getHasActiveRequests();
            users = users.filter(u -> usersWithActiveRequests.contains(u.getId()) == hasActive);
        }

        Comparator<User> comparator = switch (filter.getSortField()) {
            case ID -> Comparator.comparingLong(User::getId);
            case NAME -> Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER);
            case PHONE -> Comparator.comparing(User::getPhone);
            case VEHICLES -> Comparator.comparingLong((User u) -> vehicleCounts.getOrDefault(u.getId(), 0L));
            case REGISTERED -> Comparator.comparing(User::getCreatedAt);
        };
        return users.sorted(filter.isAscending() ? comparator : comparator.reversed()).toList();
    }

    public UserStatistics getStatistics(long userId, Period period) {
        User user = getById(userId);
        long vehicleCount = vehicleRepository.findAll().stream()
            .filter(v -> v.getUserId() == userId)
            .count();
        List<Request> requests = requestRepository.findAll().stream()
            .filter(r -> r.getUserId() == userId)
            .filter(r -> r.overlaps(period))
            .toList();
        return new UserStatistics(user, period, vehicleCount,
            Request.countByStatus(requests),
            Request.completedRevenue(requests, Spot.ratesById(spotRepository.findAll())));
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

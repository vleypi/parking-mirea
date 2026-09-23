package ru.mirea.project.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Collectors;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.ParkingRequest;
import ru.mirea.project.model.RequestStatus;
import ru.mirea.project.model.User;
import ru.mirea.project.repository.ParkingRequestRepository;

public class ParkingRequestService {
    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED_STATUS_TRANSITIONS = Map.of(
        RequestStatus.NEW, Set.of(RequestStatus.CONFIRMED, RequestStatus.CANCELLED),
        RequestStatus.CONFIRMED, Set.of(RequestStatus.COMPLETED, RequestStatus.CANCELLED),
        RequestStatus.COMPLETED, Set.of(),
        RequestStatus.CANCELLED, Set.of()
    );

    private final ParkingRequestRepository parkingRequestRepository;
    private final UserService userService;

    public ParkingRequestService(ParkingRequestRepository parkingRequestRepository, UserService userService) {
        this.parkingRequestRepository = parkingRequestRepository;
        this.userService = userService;
    }

    public ParkingRequest create(long userId, String licensePlate, int spotNumber,
                                 LocalDateTime startTime, LocalDateTime endTime) {
        String plate = validateRequestData(licensePlate, spotNumber, startTime, endTime);
        checkSpotAvailability(0, spotNumber, startTime, endTime);
        userService.getById(userId);

        ParkingRequest request = new ParkingRequest(0, userId, plate, spotNumber,
            startTime, endTime, RequestStatus.NEW, LocalDateTime.now());
        return parkingRequestRepository.create(request);
    }

    public List<ParkingRequest> getAll() {
        return parkingRequestRepository.findAll();
    }

    public List<ParkingRequest> searchByLicensePlate(String fragment) {
        String needle = InputFormats.normalizePlateText(fragment);
        return parkingRequestRepository.findAll().stream()
            .filter(r -> InputFormats.normalizePlateText(r.getLicensePlate()).contains(needle))
            .toList();
    }
    
    public Map<User, List<ParkingRequest>> searchByOwnerName(String fragment) {
        String needle = fragment.trim().toLowerCase();
        List<ParkingRequest> allRequests = parkingRequestRepository.findAll();

        return userService.getAll().stream()
            .filter(u -> u.getName().toLowerCase().contains(needle))
            .collect(Collectors.toMap(
                u -> u,
                u -> allRequests.stream().filter(r -> r.getUserId() == u.getId()).toList(),
                (a, b) -> a,
                LinkedHashMap::new));
    }

    public Statistics getStatistics() {
        List<ParkingRequest> requests = parkingRequestRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        long occupiedNow = requests.stream()
            .filter(ParkingRequestService::isActive)
            .filter(r -> !r.getStartTime().isAfter(now) && r.getEndTime().isAfter(now))
            .map(ParkingRequest::getSpotNumber)
            .distinct()
            .count();

        return new Statistics(
            userService.getAll().size(),
            requests.size(),
            requests.stream().filter(ParkingRequestService::isActive).count(),
            requests.stream().filter(r -> r.getStatus() == RequestStatus.COMPLETED).count(),
            requests.stream().filter(r -> r.getStatus() == RequestStatus.CANCELLED).count(),
            occupiedNow);
    }

    public List<ParkingRequest> filterByStatus(RequestStatus status) {
    return parkingRequestRepository.findAll().stream()
        .filter(r -> r.getStatus() == status)
        .toList();
    }

    public List<ParkingRequest> filterByDateRange(LocalDateTime from, LocalDateTime to) {
        if (from.isAfter(to)) {
            throw new BusinessException("Начало диапазона не может быть позже конца");
        }
        return parkingRequestRepository.findAll().stream()
            .filter(r -> !r.getStartTime().isBefore(from) && !r.getStartTime().isAfter(to))
            .toList();
    }

    public List<ParkingRequest> sortByStartTime(boolean ascending) {
        Comparator<ParkingRequest> comparator = Comparator.comparing(ParkingRequest::getStartTime);
        return parkingRequestRepository.findAll().stream()
            .sorted(ascending ? comparator : comparator.reversed())
            .toList();
    }

    public List<ParkingRequest> sortByCreatedAt(boolean ascending) {
        Comparator<ParkingRequest> comparator = Comparator.comparing(ParkingRequest::getCreatedAt);
        return parkingRequestRepository.findAll().stream()
            .sorted(ascending ? comparator : comparator.reversed())
            .toList();
    }
    
    public ParkingRequest getById(long id) {
        return parkingRequestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Заявка с id " + id + " не найдена"));
    }

    public ParkingRequest update(long id, String licensePlate, int spotNumber,
                                 LocalDateTime startTime, LocalDateTime endTime) {
        ParkingRequest existing = getById(id);
        String plate = validateRequestData(licensePlate, spotNumber, startTime, endTime);
        checkSpotAvailability(id, spotNumber, startTime, endTime);

        existing.setLicensePlate(plate);
        existing.setSpotNumber(spotNumber);
        existing.setStartTime(startTime);
        existing.setEndTime(endTime);
        parkingRequestRepository.update(existing);
        return existing;
    }

    public ParkingRequest changeStatus(long id, RequestStatus newStatus) {
        ParkingRequest request = getById(id);
        RequestStatus currentStatus = request.getStatus();

        if (!ALLOWED_STATUS_TRANSITIONS.get(currentStatus).contains(newStatus)) {
            throw new BusinessException("Недопустимый переход статуса: " + currentStatus + " -> " + newStatus);
        }

        request.setStatus(newStatus);
        parkingRequestRepository.update(request);
        return request;
    }

    public void delete(long id) {
        getById(id);
        parkingRequestRepository.delete(id);
    }

    public String checkLicensePlate(String licensePlate) {
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new BusinessException("Гос. номер обязателен для заполнения");
        }
        return InputFormats.normalizePlate(licensePlate);
    }

    public void checkSpotNumber(int spotNumber) {
        if (spotNumber <= 0) {
            throw new BusinessException("Номер места должен быть положительным числом");
        }
    }

    public void checkPeriod(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("Дата и время окончания должны быть позже даты и времени начала");
        }
    }

    private String validateRequestData(String licensePlate, int spotNumber, LocalDateTime startTime, LocalDateTime endTime) {
        String plate = checkLicensePlate(licensePlate);
        checkSpotNumber(spotNumber);
        checkPeriod(startTime, endTime);
        return plate;
    }

    private void checkSpotAvailability(long excludeId, int spotNumber, LocalDateTime startTime, LocalDateTime endTime) {
        boolean occupied = parkingRequestRepository.findAll().stream()
            .filter(r -> r.getId() != excludeId)
            .filter(r -> r.getSpotNumber() == spotNumber)
            .filter(ParkingRequestService::isActive)
            .anyMatch(r -> startTime.isBefore(r.getEndTime()) && endTime.isAfter(r.getStartTime()));

        if (occupied) {
            throw new BusinessException("Место " + spotNumber + " уже занято на указанный период");
        }
    }

    private static boolean isActive(ParkingRequest request) {
        return request.getStatus() == RequestStatus.NEW || request.getStatus() == RequestStatus.CONFIRMED;
    }
}

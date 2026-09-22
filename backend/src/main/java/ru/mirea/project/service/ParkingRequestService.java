package ru.mirea.project.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.ParkingRequest;
import ru.mirea.project.model.RequestStatus;
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
        validatePlateAndTimes(licensePlate, startTime, endTime);
        checkSpotAvailability(0, spotNumber, startTime, endTime);
        userService.getById(userId);

        ParkingRequest request = new ParkingRequest(0, userId, licensePlate, spotNumber,
            startTime, endTime, RequestStatus.NEW, LocalDateTime.now());
        return parkingRequestRepository.create(request);
    }

    public List<ParkingRequest> getAll() {
        return parkingRequestRepository.findAll();
    }

    public List<ParkingRequest> searchByLicensePlate(String fragment) {
    String needle = fragment.toLowerCase();
    return parkingRequestRepository.findAll().stream()
        .filter(r -> r.getLicensePlate().toLowerCase().contains(needle))
        .toList();
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
        validatePlateAndTimes(licensePlate, startTime, endTime);
        checkSpotAvailability(id, spotNumber, startTime, endTime);

        ParkingRequest existing = getById(id);
        existing.setLicensePlate(licensePlate);
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

    private void validatePlateAndTimes(String licensePlate, LocalDateTime startTime, LocalDateTime endTime) {
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new BusinessException("Гос. номер обязателен для заполнения");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("Дата и время окончания должны быть позже даты и времени начала");
        }
    }

    private void checkSpotAvailability(long excludeId, int spotNumber, LocalDateTime startTime, LocalDateTime endTime) {
        boolean occupied = parkingRequestRepository.findAll().stream()
            .filter(r -> r.getId() != excludeId)
            .filter(r -> r.getSpotNumber() == spotNumber)
            .filter(r -> r.getStatus() == RequestStatus.NEW || r.getStatus() == RequestStatus.CONFIRMED)
            .anyMatch(r -> startTime.isBefore(r.getEndTime()) && endTime.isAfter(r.getStartTime()));

        if (occupied) {
            throw new BusinessException("Место " + spotNumber + " уже занято на указанный период");
        }
    }
}

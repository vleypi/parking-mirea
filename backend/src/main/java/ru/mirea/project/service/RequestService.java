package ru.mirea.project.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ru.mirea.project.dto.RequestStatistics;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.entity.Request;
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.model.enums.RequestStatus;
import ru.mirea.project.model.enums.SpotType;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.repository.RequestRepository;

public class RequestService {
    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED_STATUS_TRANSITIONS = Map.of(
        RequestStatus.NEW, Set.of(RequestStatus.CONFIRMED, RequestStatus.CANCELLED),
        RequestStatus.CONFIRMED, Set.of(RequestStatus.COMPLETED, RequestStatus.CANCELLED),
        RequestStatus.COMPLETED, Set.of(),
        RequestStatus.CANCELLED, Set.of()
    );

    private final RequestRepository requestRepository;
    private final UserService userService;
    private final VehicleService vehicleService;
    private final SpotService spotService;

    public RequestService(RequestRepository requestRepository, UserService userService,
                                 VehicleService vehicleService, SpotService spotService) {
        this.requestRepository = requestRepository;
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.spotService = spotService;
    }

    public Request create(long userId, long vehicleId, long spotId,
                                 LocalDateTime startTime, LocalDateTime endTime) {
        userService.getById(userId);
        checkVehicleBelongsToUser(userId, vehicleId);
        Spot spot = spotService.getById(spotId);
        checkPeriod(startTime, endTime);
        checkSpotAvailability(0, spot, startTime, endTime);
        checkVehicleAvailability(0, vehicleId, startTime, endTime);

        Request request = new Request(0, userId, vehicleId, spotId,
            startTime, endTime, RequestStatus.NEW, LocalDateTime.now());
        return requestRepository.create(request);
    }

    public List<Request> getAll() {
        return requestRepository.findAll();
    }

    public List<Request> searchByLicensePlate(String fragment) {
        Set<Long> vehicleIds = vehicleService.searchByLicensePlate(fragment).stream()
            .map(Vehicle::getId)
            .collect(Collectors.toSet());
        return requestRepository.findAll().stream()
            .filter(r -> vehicleIds.contains(r.getVehicleId()))
            .toList();
    }
    
    public Map<User, List<Request>> searchByOwnerName(String fragment) {
        String needle = fragment == null ? "" : fragment.trim().toLowerCase();
        if (needle.isEmpty()) {
            throw new BusinessException("Введите фрагмент имени владельца для поиска");
        }
        List<Request> allRequests = requestRepository.findAll();

        return userService.getAll().stream()
            .filter(u -> u.getName().toLowerCase().contains(needle))
            .collect(Collectors.toMap(
                u -> u,
                u -> allRequests.stream().filter(r -> r.getUserId() == u.getId()).toList(),
                (a, b) -> a,
                LinkedHashMap::new));
    }

    public List<Request> searchBySpotNumber(int spotNumber) {
        Spot spot = spotService.findByNumber(spotNumber);
        return requestRepository.findAll().stream()
            .filter(r -> r.getSpotId() == spot.getId())
            .toList();
    }

    public List<Request> filterByPeriod(Period period) {
        return requestRepository.findAll().stream()
            .filter(r -> r.overlaps(period))
            .toList();
    }

    public List<Request> filterBySpotType(SpotType spotType) {
        Set<Long> spotIds = spotService.filterByType(spotType).stream()
            .map(Spot::getId)
            .collect(Collectors.toSet());
        return requestRepository.findAll().stream()
            .filter(r -> spotIds.contains(r.getSpotId()))
            .toList();
    }

    public RequestStatistics getStatistics(Period period) {
        List<Request> requests = filterByPeriod(period);
        List<Request> notCancelled = requests.stream().filter(r -> !r.isCancelled()).toList();
        long averageMinutes = notCancelled.isEmpty() ? 0
            : Math.round(notCancelled.stream().mapToLong(Request::durationMinutes).sum() / (double) notCancelled.size());
        return new RequestStatistics(period,
            Request.countByStatus(requests),
            Request.completedRevenue(requests, Spot.ratesById(spotService.getAll())),
            Request.hours(averageMinutes));
    }

    public List<Request> filterByStatus(RequestStatus status) {
        return requestRepository.findAll().stream()
            .filter(r -> r.getStatus() == status)
            .toList();
    }

    public List<Request> sortByStartTime(boolean ascending) {
        Comparator<Request> comparator = Comparator.comparing(Request::getStartTime);
        return requestRepository.findAll().stream()
            .sorted(ascending ? comparator : comparator.reversed())
            .toList();
    }

    public List<Request> sortByCreatedAt(boolean ascending) {
        Comparator<Request> comparator = Comparator.comparing(Request::getCreatedAt);
        return requestRepository.findAll().stream()
            .sorted(ascending ? comparator : comparator.reversed())
            .toList();
    }
    
    public Request getById(long id) {
        return requestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Заявка с id " + id + " не найдена"));
    }

    public Request update(long id, long vehicleId, long spotId,
                                 LocalDateTime startTime, LocalDateTime endTime) {
        Request existing = getById(id);
        checkEditable(existing);
        checkVehicleBelongsToUser(existing.getUserId(), vehicleId);
        Spot spot = spotService.getById(spotId);
        checkPeriod(startTime, endTime);
        checkSpotAvailability(id, spot, startTime, endTime);
        checkVehicleAvailability(id, vehicleId, startTime, endTime);

        existing.setVehicleId(vehicleId);
        existing.setSpotId(spotId);
        existing.setStartTime(startTime);
        existing.setEndTime(endTime);
        requestRepository.update(existing);
        return existing;
    }

    public Request changeStatus(long id, RequestStatus newStatus) {
        Request request = getById(id);
        RequestStatus currentStatus = request.getStatus();

        if (newStatus == null) {
            throw new BusinessException("Новый статус обязателен");
        }
        if (!ALLOWED_STATUS_TRANSITIONS.get(currentStatus).contains(newStatus)) {
            throw new BusinessException("Недопустимый переход статуса: «" + currentStatus + "» -> «" + newStatus + "»");
        }

        request.setStatus(newStatus);
        requestRepository.update(request);
        return request;
    }

    public void delete(long id) {
        getById(id);
        requestRepository.delete(id);
    }

    public void checkEditable(Request request) {
        if (!request.isActive()) {
            throw new BusinessException("Заявку в статусе «" + request.getStatus() + "» изменить нельзя: редактируются только заявки"
                + " в статусе «" + RequestStatus.NEW + "» или «" + RequestStatus.CONFIRMED + "»");
        }
    }

    public void checkPeriod(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new BusinessException("Дата и время начала и окончания обязательны");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("Дата и время окончания должны быть позже даты и времени начала");
        }
    }

    private void checkVehicleBelongsToUser(long userId, long vehicleId) {
        Vehicle vehicle = vehicleService.getById(vehicleId);
        if (vehicle.getUserId() != userId) {
            throw new BusinessException("Автомобиль " + vehicle.getLicensePlate() + " не принадлежит владельцу с id " + userId);
        }
    }

    private void checkSpotAvailability(long excludeId, Spot spot, LocalDateTime startTime, LocalDateTime endTime) {
        boolean occupied = requestRepository.findAll().stream()
            .filter(r -> r.getId() != excludeId)
            .filter(r -> r.getSpotId() == spot.getId())
            .filter(Request::isActive)
            .anyMatch(r -> r.overlaps(startTime, endTime));

        if (occupied) {
            throw new BusinessException("Место " + spot.getSpotNumber() + " уже занято на указанный период");
        }
    }

    private void checkVehicleAvailability(long excludeId, long vehicleId, LocalDateTime startTime, LocalDateTime endTime) {
        boolean busy = requestRepository.findAll().stream()
            .filter(r -> r.getId() != excludeId)
            .filter(r -> r.getVehicleId() == vehicleId)
            .filter(Request::isActive)
            .anyMatch(r -> r.overlaps(startTime, endTime));

        if (busy) {
            throw new BusinessException("Этот автомобиль уже припаркован на указанный период");
        }
    }
}

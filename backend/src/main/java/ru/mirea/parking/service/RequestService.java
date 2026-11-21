package ru.mirea.parking.service;

import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.parking.config.AppProperties;
import ru.mirea.parking.domain.Owner;
import ru.mirea.parking.domain.Request;
import ru.mirea.parking.domain.RequestStatus;
import ru.mirea.parking.domain.Spot;
import ru.mirea.parking.domain.Vehicle;
import ru.mirea.parking.dto.PageResponse;
import ru.mirea.parking.dto.request.RequestCreateRequest;
import ru.mirea.parking.dto.request.RequestResponse;
import ru.mirea.parking.dto.request.RequestUpdateRequest;
import ru.mirea.parking.filter.PageRequestFactory;
import ru.mirea.parking.filter.RequestFilter;
import ru.mirea.parking.filter.RequestSpecifications;
import ru.mirea.parking.repository.OwnerRepository;
import ru.mirea.parking.repository.RequestRepository;
import ru.mirea.parking.repository.RequestStatusRepository;
import ru.mirea.parking.repository.SpotRepository;
import ru.mirea.parking.repository.VehicleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static ru.mirea.parking.domain.RequestStatus.CANCELLED;
import static ru.mirea.parking.domain.RequestStatus.COMPLETED;
import static ru.mirea.parking.domain.RequestStatus.CONFIRMED;
import static ru.mirea.parking.domain.RequestStatus.NEW;

@Service
public class RequestService {

    static final Set<String> ACTIVE_STATUSES = Set.of(NEW, CONFIRMED);

    static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            NEW, Set.of(NEW, CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(CONFIRMED, COMPLETED, CANCELLED),
            COMPLETED, Set.of(COMPLETED),
            CANCELLED, Set.of(CANCELLED));

    public static final Set<String> SORT_FIELDS = Set.of("startTime", "cost", "createdAt", "code");

    private static final long NO_REQUEST = -1L;

    private final RequestRepository requests;
    private final OwnerRepository owners;
    private final VehicleRepository vehicles;
    private final SpotRepository spots;
    private final RequestStatusRepository statuses;
    private final RequestPriorityCalculator priorityCalculator;
    private final Clock clock;
    private final boolean permanentDeleteEnabled;

    public RequestService(RequestRepository requests, OwnerRepository owners, VehicleRepository vehicles,
                          SpotRepository spots, RequestStatusRepository statuses,
                          RequestPriorityCalculator priorityCalculator, Clock clock,
                          AppProperties properties) {
        this.requests = requests;
        this.owners = owners;
        this.vehicles = vehicles;
        this.spots = spots;
        this.statuses = statuses;
        this.priorityCalculator = priorityCalculator;
        this.clock = clock;
        this.permanentDeleteEnabled = properties.permanentDeleteEnabled();
    }

    @Transactional(readOnly = true)
    public PageResponse<RequestResponse> search(RequestFilter filter, int page, int size, String sort, String direction) {
        Pageable pageable = PageRequestFactory.create(page, size, sort, direction, SORT_FIELDS);
        var spec = RequestSpecifications.byFilter(filter, LocalDateTime.now(clock));
        return PageResponse.from(requests.findAll(spec, pageable), this::toResponse);
    }

    @Transactional(readOnly = true)
    public RequestResponse get(UUID id) {
        return toResponse(findRequest(id));
    }

    @Transactional
    public RequestResponse create(RequestCreateRequest body) {
        Owner owner = owners.findByUuid(body.ownerId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Владелец " + body.ownerId() + " не найден"));
        Vehicle vehicle = findVehicle(body.vehicleId());
        Spot spot = findSpot(body.spotId());

        checkVehicleBelongsToOwner(vehicle, owner);
        checkPeriod(body.startTime(), body.endTime());
        checkAvailability(NO_REQUEST, spot, vehicle, body.startTime(), body.endTime());

        Request request = new Request(owner, vehicle, spot, findStatus(NEW), body.startTime(), body.endTime());
        request.setDescription(body.description());
        request.setPaid(Boolean.TRUE.equals(body.paid()));
        request.setTags(normalizeTags(body.tags()));
        request.setCost(calculateCost(spot, body.startTime(), body.endTime()));
        return toResponse(requests.saveAndFlush(request));
    }

    @Transactional
    public RequestResponse update(UUID id, RequestUpdateRequest body) {
        Request request = findRequest(id);
        if (request.isArchived()) {
            throw new ResponseStatusException(CONFLICT, "Архивную заявку изменить нельзя");
        }
        if (body.version() != request.getVersion()) {
            throw new ResponseStatusException(CONFLICT, "Запись изменил другой пользователь, обновите карточку");
        }

        String currentStatus = request.getStatus().getCode();
        if (!ACTIVE_STATUSES.contains(currentStatus)) {
            throw new ResponseStatusException(CONFLICT, "Заявку в статусе «" + request.getStatus().getTitle()
                    + "» изменить нельзя: редактируются только новые и подтверждённые заявки");
        }
        RequestStatus newStatus = statuses.findByCode(body.status())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Неизвестный статус " + body.status()));
        if (!ALLOWED_TRANSITIONS.get(currentStatus).contains(newStatus.getCode())) {
            throw new ResponseStatusException(CONFLICT, "Недопустимый переход статуса: «"
                    + request.getStatus().getTitle() + "» -> «" + newStatus.getTitle() + "»");
        }

        Vehicle vehicle = findVehicle(body.vehicleId());
        Spot spot = findSpot(body.spotId());
        checkVehicleBelongsToOwner(vehicle, request.getOwner());
        checkPeriod(body.startTime(), body.endTime());
        if (ACTIVE_STATUSES.contains(newStatus.getCode())) {
            checkAvailability(request.getId(), spot, vehicle, body.startTime(), body.endTime());
        }

        request.setVehicle(vehicle);
        request.setSpot(spot);
        request.setStartTime(body.startTime());
        request.setEndTime(body.endTime());
        request.setStatus(newStatus);
        request.setDescription(body.description());
        request.setPaid(body.paid());
        request.setTags(normalizeTags(body.tags()));
        request.setCost(calculateCost(spot, body.startTime(), body.endTime()));
        try {
            return toResponse(requests.saveAndFlush(request));
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ResponseStatusException(CONFLICT, "Запись изменил другой пользователь, обновите карточку");
        }
    }

    private RequestResponse toResponse(Request request) {
        return RequestResponse.from(request, priorityCalculator.calculate(request));
    }

    static BigDecimal calculateCost(Spot spot, LocalDateTime start, LocalDateTime end) {
        long minutes = Duration.between(start, end).toMinutes();
        return spot.getHourlyRate()
                .multiply(BigDecimal.valueOf(minutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    @Transactional
    public RequestResponse archive(UUID id) {
        Request request = findRequest(id);
        archive(request);
        return toResponse(requests.saveAndFlush(request));
    }

    @Transactional
    public RequestResponse restore(UUID id) {
        Request request = findRequest(id);
        restore(request);
        return toResponse(requests.saveAndFlush(request));
    }

    @Transactional
    public void deletePermanently(UUID id) {
        if (!permanentDeleteEnabled) {
            throw new ResponseStatusException(FORBIDDEN, "Окончательное удаление отключено настройкой PERMANENT_DELETE_ENABLED");
        }
        requests.delete(findRequest(id));
    }

    void archive(Request request) {
        if (request.isArchived()) {
            throw new ResponseStatusException(CONFLICT, "Заявка " + request.getCode() + " уже в архиве");
        }
        request.setArchived(true);
    }

    void restore(Request request) {
        if (!request.isArchived()) {
            throw new ResponseStatusException(CONFLICT, "Заявка " + request.getCode() + " не в архиве");
        }
        request.setArchived(false);
    }

    private Request findRequest(UUID id) {
        return requests.findByUuid(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Заявка " + id + " не найдена"));
    }

    private Vehicle findVehicle(UUID id) {
        return vehicles.findByUuid(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Автомобиль " + id + " не найден"));
    }

    private Spot findSpot(UUID id) {
        return spots.findByUuid(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Место " + id + " не найдено"));
    }

    private RequestStatus findStatus(String code) {
        return statuses.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("В справочнике нет статуса " + code));
    }

    private void checkVehicleBelongsToOwner(Vehicle vehicle, Owner owner) {
        if (!vehicle.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(CONFLICT,
                    "Автомобиль " + vehicle.getLicensePlate() + " не принадлежит владельцу " + owner.getName());
        }
    }

    private void checkPeriod(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Дата и время окончания должны быть позже даты и времени начала");
        }
    }

    private void checkAvailability(long excludeId, Spot spot, Vehicle vehicle, LocalDateTime start, LocalDateTime end) {
        if (requests.existsSpotOverlap(spot.getId(), excludeId, ACTIVE_STATUSES, start, end)) {
            throw new ResponseStatusException(CONFLICT, "Место " + spot.getSpotNumber() + " уже занято на указанный период");
        }
        if (requests.existsVehicleOverlap(vehicle.getId(), excludeId, ACTIVE_STATUSES, start, end)) {
            throw new ResponseStatusException(CONFLICT,
                    "Автомобиль " + vehicle.getLicensePlate() + " уже стоит на парковке в указанный период");
        }
    }

    private Set<String> normalizeTags(Set<String> tags) {
        Set<String> result = new LinkedHashSet<>();
        if (tags != null) {
            tags.stream().map(String::trim).filter(t -> !t.isEmpty()).map(String::toLowerCase).forEach(result::add);
        }
        return result;
    }
}

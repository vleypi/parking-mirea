package ru.mirea.parking.service;

import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.parking.config.AppProperties;
import ru.mirea.parking.domain.AuditEntityType;
import ru.mirea.parking.domain.AuditEventType;
import ru.mirea.parking.domain.Owner;
import ru.mirea.parking.domain.Request;
import ru.mirea.parking.domain.RequestStatus;
import ru.mirea.parking.domain.Spot;
import ru.mirea.parking.domain.Vehicle;
import ru.mirea.parking.domain.VehicleStatus;
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
    private final AuditService audit;
    private final Clock clock;
    private final boolean permanentDeleteEnabled;

    public RequestService(RequestRepository requests, OwnerRepository owners, VehicleRepository vehicles,
                          SpotRepository spots, RequestStatusRepository statuses,
                          RequestPriorityCalculator priorityCalculator, AuditService audit, Clock clock,
                          AppProperties properties) {
        this.requests = requests;
        this.owners = owners;
        this.vehicles = vehicles;
        this.spots = spots;
        this.statuses = statuses;
        this.priorityCalculator = priorityCalculator;
        this.audit = audit;
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
        if (owner.isArchived()) {
            throw new ResponseStatusException(CONFLICT, "Владелец " + owner.getName() + " в архиве: новую заявку оформить нельзя");
        }
        Vehicle vehicle = findVehicle(body.vehicleId());
        Spot spot = findSpot(body.spotId());

        checkVehicleUsable(vehicle);
        checkSpotUsable(spot);
        checkVehicleBelongsToOwner(vehicle, owner);
        checkPeriod(body.startTime(), body.endTime());
        checkAvailability(NO_REQUEST, spot, vehicle, body.startTime(), body.endTime());

        Request request = new Request(owner, vehicle, spot, findStatus(NEW), body.startTime(), body.endTime());
        request.setDescription(body.description());
        request.setPaid(Boolean.TRUE.equals(body.paid()));
        request.setTags(normalizeTags(body.tags()));
        request.setCost(calculateCost(spot, body.startTime(), body.endTime()));
        Request saved = requests.saveAndFlush(request);
        audit.record(AuditEventType.CREATE, AuditEntityType.REQUEST, saved.getUuid(), "Создана заявка " + describe(saved));
        return toResponse(saved);
    }

    @Transactional
    public RequestResponse update(UUID id, RequestUpdateRequest body) {
        Request request = findRequest(id);
        checkNotArchived(request);
        if (body.version() != request.getVersion()) {
            throw new ResponseStatusException(CONFLICT, "Запись изменил другой пользователь, обновите карточку");
        }
        checkActive(request);
        RequestStatus newStatus = resolveTransition(request, body.status());

        Vehicle vehicle = findVehicle(body.vehicleId());
        Spot spot = findSpot(body.spotId());
        if (!vehicle.getId().equals(request.getVehicle().getId())) {
            checkVehicleUsable(vehicle);
        }
        if (!spot.getId().equals(request.getSpot().getId())) {
            checkSpotUsable(spot);
        }
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
            Request saved = requests.saveAndFlush(request);
            audit.record(AuditEventType.UPDATE, AuditEntityType.REQUEST, saved.getUuid(), "Изменена заявка " + describe(saved));
            return toResponse(saved);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ResponseStatusException(CONFLICT, "Запись изменил другой пользователь, обновите карточку");
        }
    }

    private RequestResponse toResponse(Request request) {
        return RequestResponse.from(request, priorityCalculator.calculate(request));
    }

    private static String describe(Request request) {
        return request.getCode() + " (" + request.getVehicle().getLicensePlate() + ", место "
                + request.getSpot().getSpotNumber() + ", " + request.getStatus().getTitle() + ")";
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
        Request request = findRequest(id);
        requests.delete(request);
        audit.record(AuditEventType.DELETE, AuditEntityType.REQUEST, request.getUuid(),
                "Заявка " + request.getCode() + " удалена окончательно");
    }

    void checkNotArchived(Request request) {
        if (request.isArchived()) {
            throw new ResponseStatusException(CONFLICT, "Архивную заявку изменить нельзя");
        }
    }

    void checkActive(Request request) {
        if (!ACTIVE_STATUSES.contains(request.getStatus().getCode())) {
            throw new ResponseStatusException(CONFLICT, "Заявку в статусе «" + request.getStatus().getTitle()
                    + "» изменить нельзя: редактируются только новые и подтверждённые заявки");
        }
    }

    RequestStatus resolveTransition(Request request, String statusCode) {
        RequestStatus newStatus = statuses.findByCode(statusCode)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Неизвестный статус " + statusCode));
        if (!ALLOWED_TRANSITIONS.get(request.getStatus().getCode()).contains(newStatus.getCode())) {
            throw new ResponseStatusException(CONFLICT, "Недопустимый переход статуса: «"
                    + request.getStatus().getTitle() + "» -> «" + newStatus.getTitle() + "»");
        }
        return newStatus;
    }

    void archive(Request request) {
        if (request.isArchived()) {
            throw new ResponseStatusException(CONFLICT, "Заявка " + request.getCode() + " уже в архиве");
        }
        request.setArchived(true);
        audit.record(AuditEventType.ARCHIVE, AuditEntityType.REQUEST, request.getUuid(),
                "Заявка " + request.getCode() + " перенесена в архив");
    }

    void restore(Request request) {
        if (!request.isArchived()) {
            throw new ResponseStatusException(CONFLICT, "Заявка " + request.getCode() + " не в архиве");
        }
        request.setArchived(false);
        audit.record(AuditEventType.RESTORE, AuditEntityType.REQUEST, request.getUuid(),
                "Заявка " + request.getCode() + " восстановлена из архива");
    }

    Request findRequest(UUID id) {
        return requests.findByUuid(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Заявка " + id + " не найдена"));
    }

    private Vehicle findVehicle(UUID id) {
        return vehicles.findByUuid(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Автомобиль " + id + " не найден"));
    }

    /** Новую заявку можно оформить только на действующий автомобиль не из архива. */
    private void checkVehicleUsable(Vehicle vehicle) {
        if (vehicle.isArchived()) {
            throw new ResponseStatusException(CONFLICT, "Автомобиль " + vehicle.getLicensePlate() + " в архиве: новую заявку оформить нельзя");
        }
        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new ResponseStatusException(CONFLICT, "Автомобиль " + vehicle.getLicensePlate() + " в статусе «"
                    + vehicle.getStatus().getTitle() + "»: новую заявку оформить нельзя");
        }
    }

    /** На место в архиве, на обслуживании или закрытое новую заявку оформить нельзя. */
    private void checkSpotUsable(Spot spot) {
        if (spot.isArchived()) {
            throw new ResponseStatusException(CONFLICT, "Место " + spot.getSpotNumber() + " в архиве: новую заявку оформить нельзя");
        }
        if (!spot.getStatus().acceptsRequests()) {
            throw new ResponseStatusException(CONFLICT, "Место " + spot.getSpotNumber() + " в статусе «"
                    + spot.getStatus().getTitle() + "»: новую заявку оформить нельзя");
        }
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
            tags.stream().map(RequestService::normalizeTag).filter(t -> !t.isEmpty()).forEach(result::add);
        }
        return result;
    }

    static String normalizeTag(String tag) {
        return tag.trim().toLowerCase();
    }
}

package ru.mirea.parking.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.parking.config.AppProperties;
import ru.mirea.parking.domain.AuditEntityType;
import ru.mirea.parking.domain.AuditEventType;
import ru.mirea.parking.domain.LicensePlates;
import ru.mirea.parking.domain.Owner;
import ru.mirea.parking.domain.Vehicle;
import ru.mirea.parking.domain.VehicleStatus;
import ru.mirea.parking.dto.PageResponse;
import ru.mirea.parking.dto.vehicle.VehicleCreateRequest;
import ru.mirea.parking.dto.vehicle.VehicleRequestsResponse;
import ru.mirea.parking.dto.vehicle.VehicleResponse;
import ru.mirea.parking.dto.vehicle.VehicleUpdateRequest;
import ru.mirea.parking.error.BadRequestException;
import ru.mirea.parking.error.ConflictException;
import ru.mirea.parking.error.ForbiddenException;
import ru.mirea.parking.error.GlobalExceptionHandler;
import ru.mirea.parking.error.NotFoundException;
import ru.mirea.parking.filter.ArchiveScope;
import ru.mirea.parking.filter.PageRequestFactory;
import ru.mirea.parking.filter.VehicleFilter;
import ru.mirea.parking.filter.VehicleSpecification;
import ru.mirea.parking.repository.OwnerRepository;
import ru.mirea.parking.repository.VehicleRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Бизнес-правила автомобиля (перенесены из VehicleService КР1/КР2): формат и уникальность номера, марка, модель, год. */
@Service
public class VehicleService {

    /** Поля сортировки API и поля сущности; гос. номер сортируется по цифровой части, как в КР2. */
    public static final Map<String, List<String>> SORT_FIELDS = Map.of(
            "licensePlate", List.of("plateDigits", "licensePlate"),
            "brand", List.of("brand", "model"),
            "releaseYear", List.of("releaseYear"),
            "createdAt", List.of("createdAt"));

    private static final int RELATED_REQUESTS = 20;

    private final VehicleRepository vehicles;
    private final OwnerRepository owners;
    private final AuditService audit;
    private final Clock clock;
    private final boolean permanentDeleteEnabled;

    public VehicleService(VehicleRepository vehicles, OwnerRepository owners, AuditService audit, Clock clock,
                          AppProperties properties) {
        this.vehicles = vehicles;
        this.owners = owners;
        this.audit = audit;
        this.clock = clock;
        this.permanentDeleteEnabled = properties.permanentDeleteEnabled();
    }

    @Transactional(readOnly = true)
    public PageResponse<VehicleResponse> search(VehicleFilter filter, int page, int size, String sort, String direction) {
        Pageable requested = PageRequestFactory.create(page, size, sort, direction, SORT_FIELDS.keySet());
        Sort.Direction dir = requested.getSort().getOrderFor(sort).getDirection();
        Sort order = Sort.by(SORT_FIELDS.get(sort).stream().map(f -> new Sort.Order(dir, f)).toList())
                .and(Sort.by(Sort.Direction.ASC, "id"));
        Pageable pageable = PageRequest.of(requested.getPageNumber(), requested.getPageSize(), order);

        Page<Vehicle> result = vehicles.findAll(specification(filter), pageable);
        Map<Long, Long> recent = recentRequests(result.getContent().stream().map(Vehicle::getId).toList());
        return PageResponse.from(result, v -> VehicleResponse.from(v, recent.getOrDefault(v.getId(), 0L)));
    }

    private Specification<Vehicle> specification(VehicleFilter filter) {
        Specification<Vehicle> spec = VehicleSpecification.byFilter(filter, now());
        if (filter.parkedFrom() == null && filter.parkedTo() == null) {
            return spec;
        }
        if (filter.parkedFrom() == null || filter.parkedTo() == null || !filter.parkedTo().isAfter(filter.parkedFrom())) {
            throw new BadRequestException("parkedTo", "Укажите начало и конец периода, конец позже начала");
        }
        return spec.and(VehicleSpecification.parkedDuring(filter.parkedFrom(), filter.parkedTo()));
    }

    /** Все строки по фильтру без страниц — для экспорта CSV. */
    @Transactional(readOnly = true)
    public List<VehicleResponse> searchAll(VehicleFilter filter) {
        List<Vehicle> list = vehicles.findAll(specification(filter), Sort.by("plateDigits", "licensePlate"));
        Map<Long, Long> recent = recentRequests(list.stream().map(Vehicle::getId).toList());
        return list.stream().map(v -> VehicleResponse.from(v, recent.getOrDefault(v.getId(), 0L))).toList();
    }

    /** Машины, стоявшие на парковке в период (неотменённые заявки, пересекающие период) — из КР2. */
    @Transactional(readOnly = true)
    public List<VehicleResponse> parkedDuring(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new BadRequestException("to", "Укажите начало и конец периода, конец позже начала");
        }
        List<Vehicle> list = vehicles.findAll(VehicleSpecification.archived(ArchiveScope.ALL)
                .and(VehicleSpecification.parkedDuring(from, to)), Sort.by("plateDigits", "licensePlate"));
        Map<Long, Long> recent = recentRequests(list.stream().map(Vehicle::getId).toList());
        return list.stream().map(v -> VehicleResponse.from(v, recent.getOrDefault(v.getId(), 0L))).toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponse get(UUID id) {
        return toResponse(findVehicle(id));
    }

    @Transactional(readOnly = true)
    public VehicleRequestsResponse requests(UUID id) {
        Vehicle vehicle = findVehicle(id);
        return new VehicleRequestsResponse(
                vehicles.findRequests(vehicle.getId(), PageRequest.of(0, RELATED_REQUESTS)).stream()
                        .map(VehicleRequestsResponse.Item::from).toList(),
                vehicles.countRequests(vehicle.getId()));
    }

    @Transactional
    public VehicleResponse create(VehicleCreateRequest body) {
        String plate = checkLicensePlate(body.licensePlate(), null);
        Owner owner = findSelectableOwner(body.ownerId());
        Vehicle vehicle = new Vehicle(owner, plate, normalizeTitle(body.brand()), normalizeTitle(body.model()));
        vehicle.setColor(normalizeColor(body.color()));
        vehicle.setReleaseYear(checkReleaseYear(body.releaseYear()));
        vehicle.setElectric(Boolean.TRUE.equals(body.electric()));
        vehicle.setStatus(body.status() == null ? VehicleStatus.ACTIVE : body.status());
        vehicle.setTags(normalizeTags(body.tags()));

        Vehicle saved = vehicles.saveAndFlush(vehicle);
        audit.record(AuditEventType.CREATE, AuditEntityType.VEHICLE, saved.getUuid(),
                "Добавлен автомобиль " + describe(saved));
        return toResponse(saved);
    }

    @Transactional
    public VehicleResponse update(UUID id, VehicleUpdateRequest body) {
        Vehicle vehicle = findVehicle(id);
        checkNotArchived(vehicle);
        if (body.version() != vehicle.getVersion()) {
            throw new ConflictException(GlobalExceptionHandler.STALE_VERSION_MESSAGE);
        }
        String plate = checkLicensePlate(body.licensePlate(), vehicle.getId());
        if (!vehicle.getOwner().getUuid().equals(body.ownerId())) {
            vehicle.setOwner(findSelectableOwner(body.ownerId()));
        }
        vehicle.setLicensePlate(plate);
        vehicle.setBrand(normalizeTitle(body.brand()));
        vehicle.setModel(normalizeTitle(body.model()));
        vehicle.setColor(normalizeColor(body.color()));
        vehicle.setReleaseYear(checkReleaseYear(body.releaseYear()));
        vehicle.setElectric(body.electric());
        vehicle.setStatus(body.status());
        vehicle.setTags(normalizeTags(body.tags()));

        Vehicle saved = vehicles.saveAndFlush(vehicle);
        audit.record(AuditEventType.UPDATE, AuditEntityType.VEHICLE, saved.getUuid(), "Изменён автомобиль " + describe(saved));
        return toResponse(saved);
    }

    /** DELETE /api/vehicles/{id}: архив вместо удаления, заявки машины остаются. */
    @Transactional
    public VehicleResponse archive(UUID id) {
        Vehicle vehicle = findVehicle(id);
        archive(vehicle);
        return toResponse(vehicles.saveAndFlush(vehicle));
    }

    @Transactional
    public VehicleResponse restore(UUID id) {
        Vehicle vehicle = findVehicle(id);
        restore(vehicle);
        return toResponse(vehicles.saveAndFlush(vehicle));
    }

    /** Окончательное удаление: Администратор, PERMANENT_DELETE_ENABLED=true и у машины нет заявок. */
    @Transactional
    public void deletePermanently(UUID id) {
        if (!permanentDeleteEnabled) {
            throw new ForbiddenException("Окончательное удаление отключено настройкой PERMANENT_DELETE_ENABLED");
        }
        Vehicle vehicle = findVehicle(id);
        if (vehicles.hasRequests(vehicle.getId())) {
            throw new ConflictException("Автомобиль " + vehicle.getLicensePlate()
                    + " нельзя удалить окончательно: по нему есть заявки. Перенесите его в архив");
        }
        audit.record(AuditEventType.DELETE, AuditEntityType.VEHICLE, vehicle.getUuid(),
                "Окончательно удалён автомобиль " + describe(vehicle));
        vehicles.delete(vehicle);
        vehicles.flush();
    }

    void archive(Vehicle vehicle) {
        if (vehicle.isArchived()) {
            throw new ConflictException("Автомобиль " + vehicle.getLicensePlate() + " уже в архиве");
        }
        vehicle.setArchived(true);
        audit.record(AuditEventType.ARCHIVE, AuditEntityType.VEHICLE, vehicle.getUuid(),
                "Автомобиль " + vehicle.getLicensePlate() + " перенесён в архив");
    }

    void restore(Vehicle vehicle) {
        if (!vehicle.isArchived()) {
            throw new ConflictException("Автомобиль " + vehicle.getLicensePlate() + " не в архиве");
        }
        vehicle.setArchived(false);
        audit.record(AuditEventType.RESTORE, AuditEntityType.VEHICLE, vehicle.getUuid(),
                "Автомобиль " + vehicle.getLicensePlate() + " восстановлен из архива");
    }

    void checkNotArchived(Vehicle vehicle) {
        if (vehicle.isArchived()) {
            throw new ConflictException("Архивный автомобиль изменить нельзя");
        }
    }

    Vehicle findVehicle(UUID id) {
        return vehicles.findByUuid(id).orElseThrow(() -> new NotFoundException("Автомобиль " + id + " не найден"));
    }

    VehicleResponse toResponse(Vehicle vehicle) {
        return VehicleResponse.from(vehicle, recentRequests(List.of(vehicle.getId())).getOrDefault(vehicle.getId(), 0L));
    }

    /** Заявки за окно активности по каждой машине одним GROUP BY, без запроса на каждую строку. */
    Map<Long, Long> recentRequests(Collection<Long> vehicleIds) {
        Map<Long, Long> result = new HashMap<>();
        if (vehicleIds.isEmpty()) {
            return result;
        }
        for (Object[] row : vehicles.countRecentRequests(vehicleIds, VehicleActivityCalculator.windowStart(now()))) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    /** checkLicensePlate из КР1: нормализация и уникальность. */
    String checkLicensePlate(String raw, Long ownId) {
        String plate = LicensePlates.normalizeOrNull(raw);
        if (plate == null) {
            throw new BadRequestException("licensePlate", LicensePlates.FORMAT_MESSAGE);
        }
        boolean taken = ownId == null ? vehicles.existsByLicensePlate(plate) : vehicles.existsByLicensePlateAndIdNot(plate, ownId);
        if (taken) {
            throw new ConflictException("Автомобиль с гос. номером " + plate + " уже есть");
        }
        return plate;
    }

    /** Новую машину нельзя оформить на архивного владельца. */
    Owner findSelectableOwner(UUID ownerId) {
        Owner owner = owners.findByUuid(ownerId).orElseThrow(() -> new NotFoundException("Владелец " + ownerId + " не найден"));
        if (owner.isArchived()) {
            throw new ConflictException("Владелец " + owner.getName() + " в архиве: оформить на него автомобиль нельзя");
        }
        return owner;
    }

    Short checkReleaseYear(Short year) {
        if (year != null && year > Year.now(clock).getValue()) {
            throw new BadRequestException("releaseYear", "Год выпуска не позже " + Year.now(clock).getValue());
        }
        return year;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /** checkBrand/checkModel из КР1: пробелы схлопываются, первая буква заглавная. */
    static String normalizeTitle(String raw) {
        String text = raw.trim().replaceAll("\\s+", " ");
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String normalizeColor(String color) {
        return color == null || color.isBlank() ? null : color.trim().toLowerCase(Locale.ROOT);
    }

    static Set<String> normalizeTags(Set<String> tags) {
        Set<String> result = new LinkedHashSet<>();
        if (tags != null) {
            tags.stream().map(VehicleService::normalizeTag).filter(t -> !t.isEmpty()).forEach(result::add);
        }
        return result;
    }

    static String normalizeTag(String tag) {
        return tag.trim().toLowerCase(Locale.ROOT);
    }

    private static String describe(Vehicle vehicle) {
        return vehicle.getLicensePlate() + " (" + vehicle.getBrand() + " " + vehicle.getModel() + ")";
    }
}

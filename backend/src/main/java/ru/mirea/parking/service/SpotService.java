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
import ru.mirea.parking.domain.Spot;
import ru.mirea.parking.domain.SpotStatus;
import ru.mirea.parking.domain.SpotType;
import ru.mirea.parking.dto.PageResponse;
import ru.mirea.parking.dto.spot.SpotCreateRequest;
import ru.mirea.parking.dto.spot.SpotRequestsResponse;
import ru.mirea.parking.dto.spot.SpotResponse;
import ru.mirea.parking.dto.spot.SpotUpdateRequest;
import ru.mirea.parking.error.BadRequestException;
import ru.mirea.parking.error.ConflictException;
import ru.mirea.parking.error.ForbiddenException;
import ru.mirea.parking.error.GlobalExceptionHandler;
import ru.mirea.parking.error.NotFoundException;
import ru.mirea.parking.filter.ArchiveScope;
import ru.mirea.parking.filter.PageRequestFactory;
import ru.mirea.parking.filter.SpotFilter;
import ru.mirea.parking.filter.SpotSpecification;
import ru.mirea.parking.repository.SpotRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Бизнес-правила парковочного места (перенесены из SpotService КР1/КР2): номер, тип, тариф, доступность. */
@Service
public class SpotService {

    /** Поля сортировки API и поля сущности; при равенстве — по номеру места. */
    public static final Map<String, List<String>> SORT_FIELDS = Map.of(
            "spotNumber", List.of("spotNumber"),
            "hourlyRate", List.of("hourlyRate", "spotNumber"),
            "zone", List.of("zone", "spotNumber"),
            "createdAt", List.of("createdAt"));

    private static final int RELATED_REQUESTS = 20;

    private final SpotRepository spots;
    private final ReferenceService references;
    private final AuditService audit;
    private final Clock clock;
    private final boolean permanentDeleteEnabled;

    public SpotService(SpotRepository spots, ReferenceService references, AuditService audit, Clock clock,
                       AppProperties properties) {
        this.spots = spots;
        this.references = references;
        this.audit = audit;
        this.clock = clock;
        this.permanentDeleteEnabled = properties.permanentDeleteEnabled();
    }

    @Transactional(readOnly = true)
    public PageResponse<SpotResponse> search(SpotFilter filter, int page, int size, String sort, String direction) {
        Pageable requested = PageRequestFactory.create(page, size, sort, direction, SORT_FIELDS.keySet());
        Sort.Direction dir = requested.getSort().getOrderFor(sort).getDirection();
        Sort order = Sort.by(SORT_FIELDS.get(sort).stream().map(f -> new Sort.Order(dir, f)).toList())
                .and(Sort.by(Sort.Direction.ASC, "id"));
        Pageable pageable = PageRequest.of(requested.getPageNumber(), requested.getPageSize(), order);

        Page<Spot> result = spots.findAll(specification(filter), pageable);
        Set<Long> occupied = occupiedNow(result.getContent().stream().map(Spot::getId).toList());
        return PageResponse.from(result, s -> SpotResponse.from(s, occupied.contains(s.getId())));
    }

    private Specification<Spot> specification(SpotFilter filter) {
        Specification<Spot> spec = SpotSpecification.byFilter(filter, now());
        if (filter.freeFrom() == null && filter.freeTo() == null) {
            return spec;
        }
        if (filter.freeFrom() == null || filter.freeTo() == null || !filter.freeTo().isAfter(filter.freeFrom())) {
            throw new BadRequestException("freeTo", "Укажите начало и конец периода, конец позже начала");
        }
        return spec.and(SpotSpecification.freeDuring(filter.freeFrom(), filter.freeTo()));
    }

    /** Все строки по фильтру без страниц — для экспорта CSV. */
    @Transactional(readOnly = true)
    public List<SpotResponse> searchAll(SpotFilter filter) {
        return toResponses(spots.findAll(specification(filter), Sort.by("spotNumber")));
    }

    /** Свободные места на период (findFreeDuring из КР2): работающие, не в архиве, без активной заявки в периоде. */
    @Transactional(readOnly = true)
    public List<SpotResponse> freeDuring(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new BadRequestException("to", "Укажите начало и конец периода, конец позже начала");
        }
        return toResponses(spots.findAll(SpotSpecification.archived(ArchiveScope.ACTIVE)
                .and(SpotSpecification.freeDuring(from, to)), Sort.by("spotNumber")));
    }

    @Transactional(readOnly = true)
    public SpotResponse get(UUID id) {
        return toResponse(findSpot(id));
    }

    @Transactional(readOnly = true)
    public SpotRequestsResponse requests(UUID id) {
        Spot spot = findSpot(id);
        return new SpotRequestsResponse(
                spots.findRequests(spot.getId(), PageRequest.of(0, RELATED_REQUESTS)).stream()
                        .map(SpotRequestsResponse.Item::from).toList(),
                spots.countRequests(spot.getId()));
    }

    @Transactional
    public SpotResponse create(SpotCreateRequest body) {
        int number = checkSpotNumberUnique(body.spotNumber(), null);
        SpotType type = references.selectableSpotType(body.spotTypeId(), null);
        Spot spot = new Spot(number, type, normalizeRate(body.hourlyRate()));
        spot.setZone(blankToNull(body.zone()));
        spot.setDescription(blankToNull(body.description()));
        spot.setStatus(body.status() == null ? SpotStatus.AVAILABLE : body.status());
        spot.setCovered(Boolean.TRUE.equals(body.covered()));
        spot.setTags(normalizeTags(body.tags()));

        Spot saved = spots.saveAndFlush(spot);
        audit.record(AuditEventType.CREATE, AuditEntityType.SPOT, saved.getUuid(), "Добавлено место " + describe(saved));
        return toResponse(saved);
    }

    @Transactional
    public SpotResponse update(UUID id, SpotUpdateRequest body) {
        Spot spot = findSpot(id);
        checkNotArchived(spot);
        if (body.version() != spot.getVersion()) {
            throw new ConflictException(GlobalExceptionHandler.STALE_VERSION_MESSAGE);
        }
        spot.setSpotNumber(checkSpotNumberUnique(body.spotNumber(), spot.getId()));
        spot.setSpotType(references.selectableSpotType(body.spotTypeId(), spot.getSpotType()));
        spot.setHourlyRate(normalizeRate(body.hourlyRate()));
        spot.setZone(blankToNull(body.zone()));
        spot.setDescription(blankToNull(body.description()));
        spot.setStatus(body.status());
        spot.setCovered(body.covered());
        spot.setTags(normalizeTags(body.tags()));

        Spot saved = spots.saveAndFlush(spot);
        audit.record(AuditEventType.UPDATE, AuditEntityType.SPOT, saved.getUuid(), "Изменено место " + describe(saved));
        return toResponse(saved);
    }

    /** DELETE /api/spots/{id}: архив вместо удаления, если на место нет незавершённых активных заявок. */
    @Transactional
    public SpotResponse archive(UUID id) {
        Spot spot = findSpot(id);
        archive(spot);
        return toResponse(spots.saveAndFlush(spot));
    }

    @Transactional
    public SpotResponse restore(UUID id) {
        Spot spot = findSpot(id);
        restore(spot);
        return toResponse(spots.saveAndFlush(spot));
    }

    /** Окончательное удаление: Администратор, PERMANENT_DELETE_ENABLED=true и на место нет заявок. */
    @Transactional
    public void deletePermanently(UUID id) {
        if (!permanentDeleteEnabled) {
            throw new ForbiddenException("Окончательное удаление отключено настройкой PERMANENT_DELETE_ENABLED");
        }
        Spot spot = findSpot(id);
        if (spots.hasRequests(spot.getId())) {
            throw new ConflictException("Место " + spot.getSpotNumber()
                    + " нельзя удалить окончательно: на него есть заявки. Перенесите его в архив");
        }
        audit.record(AuditEventType.DELETE, AuditEntityType.SPOT, spot.getUuid(), "Окончательно удалено место " + describe(spot));
        spots.delete(spot);
        spots.flush();
    }

    void archive(Spot spot) {
        if (spot.isArchived()) {
            throw new ConflictException("Место " + spot.getSpotNumber() + " уже в архиве");
        }
        if (spots.hasActiveRequestsAfter(spot.getId(), now())) {
            throw new ConflictException("Место " + spot.getSpotNumber()
                    + " нельзя перенести в архив: на него есть новая или подтверждённая заявка, которая ещё не закончилась");
        }
        spot.setArchived(true);
        audit.record(AuditEventType.ARCHIVE, AuditEntityType.SPOT, spot.getUuid(),
                "Место " + spot.getSpotNumber() + " перенесено в архив");
    }

    void restore(Spot spot) {
        if (!spot.isArchived()) {
            throw new ConflictException("Место " + spot.getSpotNumber() + " не в архиве");
        }
        spot.setArchived(false);
        audit.record(AuditEventType.RESTORE, AuditEntityType.SPOT, spot.getUuid(),
                "Место " + spot.getSpotNumber() + " восстановлено из архива");
    }

    void checkNotArchived(Spot spot) {
        if (spot.isArchived()) {
            throw new ConflictException("Архивное место изменить нельзя");
        }
    }

    Spot findSpot(UUID id) {
        return spots.findByUuid(id).orElseThrow(() -> new NotFoundException("Место " + id + " не найдено"));
    }

    SpotResponse toResponse(Spot spot) {
        return SpotResponse.from(spot, occupiedNow(List.of(spot.getId())).contains(spot.getId()));
    }

    private List<SpotResponse> toResponses(List<Spot> list) {
        Set<Long> occupied = occupiedNow(list.stream().map(Spot::getId).toList());
        return list.stream().map(s -> SpotResponse.from(s, occupied.contains(s.getId()))).toList();
    }

    /** Занятые сейчас места из переданных — одним запросом, без запроса на каждую строку. */
    Set<Long> occupiedNow(Collection<Long> spotIds) {
        return spotIds.isEmpty() ? Set.of() : new HashSet<>(spots.findOccupiedAt(spotIds, now()));
    }

    /** checkSpotNumberUnique из КР1. Положительность проверяет Bean Validation. */
    int checkSpotNumberUnique(int number, Long ownId) {
        boolean taken = ownId == null ? spots.existsBySpotNumber(number) : spots.existsBySpotNumberAndIdNot(number, ownId);
        if (taken) {
            throw new ConflictException("Место с номером " + number + " уже есть");
        }
        return number;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /** parseHourlyRate из КР1: два знака после запятой. Диапазон проверяет Bean Validation. */
    static BigDecimal normalizeRate(BigDecimal rate) {
        return rate.setScale(2, RoundingMode.HALF_UP);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().replaceAll("\\s+", " ");
    }

    static Set<String> normalizeTags(Set<String> tags) {
        Set<String> result = new LinkedHashSet<>();
        if (tags != null) {
            tags.stream().map(SpotService::normalizeTag).filter(t -> !t.isEmpty()).forEach(result::add);
        }
        return result;
    }

    static String normalizeTag(String tag) {
        return tag.trim().toLowerCase(Locale.ROOT);
    }

    private static String describe(Spot spot) {
        return "№" + spot.getSpotNumber() + " (" + spot.getSpotType().getTitle() + ", " + spot.getHourlyRate() + " ₽/ч)";
    }
}

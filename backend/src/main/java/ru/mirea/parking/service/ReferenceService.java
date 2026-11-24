package ru.mirea.parking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.parking.domain.AuditEntityType;
import ru.mirea.parking.domain.AuditEventType;
import ru.mirea.parking.domain.OwnerStatus;
import ru.mirea.parking.domain.SpotStatus;
import ru.mirea.parking.domain.SpotType;
import ru.mirea.parking.domain.VehicleStatus;
import ru.mirea.parking.dto.reference.ClientCategoryResponse;
import ru.mirea.parking.dto.reference.ReferencesResponse;
import ru.mirea.parking.dto.reference.ReferencesResponse.CodeTitle;
import ru.mirea.parking.dto.reference.SpotTypeRequest;
import ru.mirea.parking.dto.reference.SpotTypeResponse;
import ru.mirea.parking.error.BadRequestException;
import ru.mirea.parking.error.ConflictException;
import ru.mirea.parking.error.NotFoundException;
import ru.mirea.parking.repository.ClientCategoryRepository;
import ru.mirea.parking.repository.RequestStatusRepository;
import ru.mirea.parking.repository.SpotTypeRepository;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Справочники: общий список для форм и управление типами мест администратором. */
@Service
public class ReferenceService {

    private final SpotTypeRepository spotTypes;
    private final RequestStatusRepository requestStatuses;
    private final ClientCategoryRepository clientCategories;
    private final AuditService audit;

    public ReferenceService(SpotTypeRepository spotTypes, RequestStatusRepository requestStatuses,
                            ClientCategoryRepository clientCategories, AuditService audit) {
        this.spotTypes = spotTypes;
        this.requestStatuses = requestStatuses;
        this.clientCategories = clientCategories;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public ReferencesResponse all() {
        return new ReferencesResponse(
                spotTypes(),
                requestStatuses.findAll().stream()
                        .sorted((a, b) -> a.getId().compareTo(b.getId()))
                        .map(s -> new CodeTitle(s.getCode(), s.getTitle())).toList(),
                clientCategories.findAllByOrderByIdAsc().stream().map(ClientCategoryResponse::from).toList(),
                Arrays.stream(OwnerStatus.values()).map(s -> new CodeTitle(s.name(), s.getTitle())).toList(),
                Arrays.stream(VehicleStatus.values()).map(s -> new CodeTitle(s.name(), s.getTitle())).toList(),
                Arrays.stream(SpotStatus.values()).map(s -> new CodeTitle(s.name(), s.getTitle())).toList());
    }

    @Transactional(readOnly = true)
    public List<SpotTypeResponse> spotTypes() {
        return spotTypes.findAllByOrderBySortOrderAscIdAsc().stream().map(SpotTypeResponse::from).toList();
    }

    @Transactional
    public SpotTypeResponse createSpotType(SpotTypeRequest body) {
        String code = normalizeCode(body.code());
        String title = body.title().trim();
        checkUnique(code, title, (short) -1);
        short id = (short) spotTypes.nextId();
        SpotType type = new SpotType(id, code, title, body.sortOrder() == null ? (short) (id * 10) : body.sortOrder());
        SpotType saved = spotTypes.saveAndFlush(type);
        record(AuditEventType.CREATE, saved, "Добавлен тип места «" + saved.getTitle() + "» (" + saved.getCode() + ")");
        return SpotTypeResponse.from(saved);
    }

    @Transactional
    public SpotTypeResponse updateSpotType(short id, SpotTypeRequest body) {
        SpotType type = findSpotType(id);
        String code = normalizeCode(body.code());
        String title = body.title().trim();
        checkUnique(code, title, id);
        type.setCode(code);
        type.setTitle(title);
        if (body.sortOrder() != null) {
            type.setSortOrder(body.sortOrder());
        }
        SpotType saved = spotTypes.saveAndFlush(type);
        record(AuditEventType.UPDATE, saved, "Изменён тип места «" + saved.getTitle() + "» (" + saved.getCode() + ")");
        return SpotTypeResponse.from(saved);
    }

    @Transactional
    public SpotTypeResponse setSpotTypeActive(short id, boolean active) {
        SpotType type = findSpotType(id);
        type.setActive(active);
        SpotType saved = spotTypes.saveAndFlush(type);
        record(AuditEventType.UPDATE, saved, "Тип места «" + saved.getTitle() + "» " + (active ? "включён" : "отключён"));
        return SpotTypeResponse.from(saved);
    }

    /** Тип места для новой или изменённой записи: отключённый можно оставить только тот, что уже стоит у места. */
    @Transactional(readOnly = true)
    public SpotType selectableSpotType(Short id, SpotType current) {
        SpotType type = spotTypes.findById(id)
                .orElseThrow(() -> new BadRequestException("spotTypeId", "Тип места " + id + " не найден"));
        boolean unchanged = current != null && current.getId().equals(type.getId());
        if (!type.isActive() && !unchanged) {
            throw new BadRequestException("spotTypeId", "Тип места «" + type.getTitle() + "» отключён");
        }
        return type;
    }

    SpotType findSpotType(short id) {
        return spotTypes.findById(id).orElseThrow(() -> new NotFoundException("Тип места " + id + " не найден"));
    }

    private void checkUnique(String code, String title, short ownId) {
        if (spotTypes.existsByCodeIgnoreCaseAndIdNot(code, ownId)) {
            throw new ConflictException("Тип места с кодом " + code + " уже есть");
        }
        if (spotTypes.existsByTitleIgnoreCaseAndIdNot(title, ownId)) {
            throw new ConflictException("Тип места «" + title + "» уже есть");
        }
    }

    private void record(AuditEventType type, SpotType spotType, String summary) {
        UUID entityId = UUID.nameUUIDFromBytes(("spot_type:" + spotType.getId()).getBytes(StandardCharsets.UTF_8));
        audit.record(type, AuditEntityType.REFERENCE, entityId, summary);
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}

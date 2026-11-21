package ru.mirea.parking.service;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.parking.config.AppProperties;
import ru.mirea.parking.domain.AuditEntityType;
import ru.mirea.parking.domain.AuditEventType;
import ru.mirea.parking.domain.ClientCategory;
import ru.mirea.parking.domain.Owner;
import ru.mirea.parking.domain.OwnerStatus;
import ru.mirea.parking.domain.RequestStatus;
import ru.mirea.parking.dto.owner.OwnerCreateRequest;
import ru.mirea.parking.dto.owner.OwnerResponse;
import ru.mirea.parking.dto.owner.OwnerResponse.Counters;
import ru.mirea.parking.dto.owner.OwnerUpdateRequest;
import ru.mirea.parking.repository.ClientCategoryRepository;
import ru.mirea.parking.repository.OwnerRepository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Бизнес-правила владельца (перенесены из UserService КР1):
 * имя и телефон обязательны, телефон уникален, категория из справочника и не отключена.
 */
@Service
public class OwnerService {

    static final String STALE_VERSION = "Запись изменил другой пользователь, обновите карточку";

    private final OwnerRepository owners;
    private final ClientCategoryRepository categories;
    private final AuditService audit;
    private final boolean permanentDeleteEnabled;

    public OwnerService(OwnerRepository owners, ClientCategoryRepository categories, AuditService audit,
                        AppProperties properties) {
        this.owners = owners;
        this.categories = categories;
        this.audit = audit;
        this.permanentDeleteEnabled = properties.permanentDeleteEnabled();
    }

    @Transactional(readOnly = true)
    public OwnerResponse get(UUID id) {
        return toResponse(findOwner(id));
    }

    @Transactional
    public OwnerResponse create(OwnerCreateRequest body) {
        String phone = body.phone().trim();
        checkPhoneFree(phone, null);
        Owner owner = new Owner(body.name().trim(), phone, findSelectableCategory(body.categoryId(), null));
        owner.setEmail(normalizeEmail(body.email()));
        owner.setStatus(body.status() == null ? OwnerStatus.ACTIVE : body.status());
        owner.setDiscountPercent(body.discountPercent() == null ? BigDecimal.ZERO : body.discountPercent());
        owner.setNotificationsEnabled(body.notificationsEnabled() == null || body.notificationsEnabled());
        owner.setTags(normalizeTags(body.tags()));

        Owner saved = owners.saveAndFlush(owner);
        audit.record(AuditEventType.CREATE, AuditEntityType.OWNER, saved.getUuid(),
                "Создан владелец " + saved.getName() + " (" + saved.getPhone() + ")");
        return toResponse(saved);
    }

    @Transactional
    public OwnerResponse update(UUID id, OwnerUpdateRequest body) {
        Owner owner = findOwner(id);
        checkNotArchived(owner);
        if (body.version() != owner.getVersion()) {
            throw new ResponseStatusException(CONFLICT, STALE_VERSION);
        }
        String phone = body.phone().trim();
        checkPhoneFree(phone, owner.getId());

        owner.setName(body.name().trim());
        owner.setPhone(phone);
        owner.setEmail(normalizeEmail(body.email()));
        owner.setCategory(findSelectableCategory(body.categoryId(), owner.getCategory()));
        owner.setStatus(body.status());
        owner.setDiscountPercent(body.discountPercent());
        owner.setNotificationsEnabled(body.notificationsEnabled());
        owner.setTags(normalizeTags(body.tags()));
        try {
            Owner saved = owners.saveAndFlush(owner);
            audit.record(AuditEventType.UPDATE, AuditEntityType.OWNER, saved.getUuid(),
                    "Изменён владелец " + saved.getName() + " (" + saved.getPhone() + ")");
            return toResponse(saved);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ResponseStatusException(CONFLICT, STALE_VERSION);
        }
    }

    /** DELETE /api/owners/{id}: архив вместо удаления. Связанные машины и заявки остаются. */
    @Transactional
    public OwnerResponse archive(UUID id) {
        Owner owner = findOwner(id);
        archive(owner);
        return toResponse(owners.saveAndFlush(owner));
    }

    @Transactional
    public OwnerResponse restore(UUID id) {
        Owner owner = findOwner(id);
        restore(owner);
        return toResponse(owners.saveAndFlush(owner));
    }

    /**
     * Окончательное удаление: только Администратор (проверяет контроллер), только при PERMANENT_DELETE_ENABLED=true
     * и только если у владельца нет машин и заявок (правило КР2). Теги и заметки удаляются каскадом.
     */
    @Transactional
    public void deletePermanently(UUID id) {
        if (!permanentDeleteEnabled) {
            throw new ResponseStatusException(FORBIDDEN, "Окончательное удаление отключено настройкой PERMANENT_DELETE_ENABLED");
        }
        Owner owner = findOwner(id);
        if (owners.hasVehicles(owner.getId()) || owners.hasRequests(owner.getId())) {
            throw new ResponseStatusException(CONFLICT,
                    "Владельца " + owner.getName() + " нельзя удалить окончательно: у владельца есть автомобили или заявки");
        }
        audit.record(AuditEventType.DELETE, AuditEntityType.OWNER, owner.getUuid(),
                "Окончательно удалён владелец " + owner.getName() + " (" + owner.getPhone() + ")");
        owners.delete(owner);
        owners.flush();
    }

    void archive(Owner owner) {
        if (owner.isArchived()) {
            throw new ResponseStatusException(CONFLICT, "Владелец " + owner.getName() + " уже в архиве");
        }
        owner.setArchived(true);
        audit.record(AuditEventType.ARCHIVE, AuditEntityType.OWNER, owner.getUuid(),
                "Владелец " + owner.getName() + " перенесён в архив");
    }

    void restore(Owner owner) {
        if (!owner.isArchived()) {
            throw new ResponseStatusException(CONFLICT, "Владелец " + owner.getName() + " не в архиве");
        }
        owner.setArchived(false);
        audit.record(AuditEventType.RESTORE, AuditEntityType.OWNER, owner.getUuid(),
                "Владелец " + owner.getName() + " восстановлен из архива");
    }

    void checkNotArchived(Owner owner) {
        if (owner.isArchived()) {
            throw new ResponseStatusException(CONFLICT, "Архивного владельца изменить нельзя");
        }
    }

    OwnerResponse toResponse(Owner owner) {
        return OwnerResponse.from(owner, counters(List.of(owner.getId())).getOrDefault(owner.getId(), Counters.ZERO));
    }

    /**
     * Машины и завершённые заявки для списка владельцев: два GROUP BY на всю страницу вместо запросов на каждую строку.
     * Количество завершённых заявок даёт уровень лояльности.
     */
    Map<Long, Counters> counters(Collection<Long> ownerIds) {
        Map<Long, Counters> result = new HashMap<>();
        if (ownerIds.isEmpty()) {
            return result;
        }
        Map<Long, Long> vehicles = toMap(owners.countVehiclesByOwnerIds(ownerIds));
        Map<Long, Long> completed = toMap(owners.countRequestsByOwnerIds(ownerIds, RequestStatus.COMPLETED));
        for (Long id : ownerIds) {
            result.put(id, new Counters(vehicles.getOrDefault(id, 0L), completed.getOrDefault(id, 0L)));
        }
        return result;
    }

    private static Map<Long, Long> toMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    Owner findOwner(UUID id) {
        return owners.findByUuid(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Владелец " + id + " не найден"));
    }

    private void checkPhoneFree(String phone, Long ownId) {
        boolean taken = ownId == null ? owners.existsByPhone(phone) : owners.existsByPhoneAndIdNot(phone, ownId);
        if (taken) {
            throw new ResponseStatusException(CONFLICT, "Телефон " + phone + " уже указан у другого владельца");
        }
    }

    /**
     * Категория из справочника. Отключённую нельзя выбрать заново,
     * но если она уже стоит у владельца, его можно сохранить без смены категории.
     */
    private ClientCategory findSelectableCategory(Short id, ClientCategory current) {
        ClientCategory category = categories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Категория клиента " + id + " не найдена"));
        boolean unchanged = current != null && current.getId().equals(category.getId());
        if (!category.isActive() && !unchanged) {
            throw new ResponseStatusException(BAD_REQUEST, "Категория «" + category.getTitle() + "» отключена");
        }
        return category;
    }

    private static String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static Set<String> normalizeTags(Set<String> tags) {
        Set<String> result = new LinkedHashSet<>();
        if (tags != null) {
            tags.stream().map(OwnerService::normalizeTag).filter(t -> !t.isEmpty()).forEach(result::add);
        }
        return result;
    }

    static String normalizeTag(String tag) {
        return tag.trim().toLowerCase(Locale.ROOT);
    }
}

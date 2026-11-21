package ru.mirea.parking.service;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.parking.domain.AuditEntityType;
import ru.mirea.parking.domain.AuditEventType;
import ru.mirea.parking.domain.ClientCategory;
import ru.mirea.parking.domain.Owner;
import ru.mirea.parking.domain.OwnerStatus;
import ru.mirea.parking.dto.owner.OwnerCreateRequest;
import ru.mirea.parking.dto.owner.OwnerResponse;
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

    public OwnerService(OwnerRepository owners, ClientCategoryRepository categories, AuditService audit) {
        this.owners = owners;
        this.categories = categories;
        this.audit = audit;
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

    OwnerResponse toResponse(Owner owner) {
        return OwnerResponse.from(owner, countVehicles(List.of(owner.getId())).getOrDefault(owner.getId(), 0L));
    }

    /** Количество машин для страницы владельцев: один GROUP BY вместо запроса на каждую строку. */
    Map<Long, Long> countVehicles(Collection<Long> ownerIds) {
        Map<Long, Long> result = new HashMap<>();
        if (!ownerIds.isEmpty()) {
            for (Object[] row : owners.countVehiclesByOwnerIds(ownerIds)) {
                result.put((Long) row[0], (Long) row[1]);
            }
        }
        return result;
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

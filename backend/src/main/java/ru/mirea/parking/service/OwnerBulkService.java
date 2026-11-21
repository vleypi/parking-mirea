package ru.mirea.parking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.parking.domain.AuditEntityType;
import ru.mirea.parking.domain.AuditEventType;
import ru.mirea.parking.domain.Owner;
import ru.mirea.parking.dto.owner.OwnerBulkRequest;
import ru.mirea.parking.dto.owner.OwnerBulkResponse;
import ru.mirea.parking.dto.request.BulkAction;
import ru.mirea.parking.repository.OwnerRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Массовые операции над владельцами. Каждая запись обрабатывается в своей транзакции:
 * ошибка по одной записи (нет такого UUID, уже в архиве и т.п.) не отменяет остальные.
 */
@Service
public class OwnerBulkService {

    private static final Set<BulkAction> TAG_ACTIONS = Set.of(BulkAction.ADD_TAG, BulkAction.REMOVE_TAG);

    private final OwnerService ownerService;
    private final OwnerRepository owners;
    private final AuditService audit;
    private final TransactionTemplate transaction;

    public OwnerBulkService(OwnerService ownerService, OwnerRepository owners, AuditService audit,
                            PlatformTransactionManager transactionManager) {
        this.ownerService = ownerService;
        this.owners = owners;
        this.audit = audit;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public OwnerBulkResponse apply(OwnerBulkRequest body) {
        Consumer<Owner> action = toAction(body);
        List<UUID> succeeded = new ArrayList<>();
        List<OwnerBulkResponse.Failure> failed = new ArrayList<>();

        for (UUID id : new LinkedHashSet<>(body.ids())) {
            try {
                transaction.executeWithoutResult(status -> {
                    Owner owner = ownerService.findOwner(id);
                    action.accept(owner);
                    owners.saveAndFlush(owner);
                });
                succeeded.add(id);
            } catch (ResponseStatusException e) {
                failed.add(new OwnerBulkResponse.Failure(id, e.getReason()));
            }
        }
        return new OwnerBulkResponse(succeeded, failed);
    }

    private Consumer<Owner> toAction(OwnerBulkRequest body) {
        if (body.action() == BulkAction.CHANGE_STATUS && body.status() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Для смены статуса укажите status (ACTIVE или BLOCKED)");
        }
        if (TAG_ACTIONS.contains(body.action()) && (body.tag() == null || body.tag().isBlank())) {
            throw new ResponseStatusException(BAD_REQUEST, "Для действия с тегом укажите tag");
        }
        return switch (body.action()) {
            case ARCHIVE -> ownerService::archive;
            case RESTORE -> ownerService::restore;
            case CHANGE_STATUS -> owner -> {
                ownerService.checkNotArchived(owner);
                owner.setStatus(body.status());
                audit.record(AuditEventType.UPDATE, AuditEntityType.OWNER, owner.getUuid(),
                        "Владельцу " + owner.getName() + " установлен статус «" + body.status().getTitle() + "»");
            };
            case ADD_TAG -> owner -> {
                ownerService.checkNotArchived(owner);
                String tag = OwnerService.normalizeTag(body.tag());
                owner.getTags().add(tag);
                audit.record(AuditEventType.UPDATE, AuditEntityType.OWNER, owner.getUuid(),
                        "Владельцу " + owner.getName() + " добавлен тег «" + tag + "»");
            };
            case REMOVE_TAG -> owner -> {
                ownerService.checkNotArchived(owner);
                String tag = OwnerService.normalizeTag(body.tag());
                owner.getTags().remove(tag);
                audit.record(AuditEventType.UPDATE, AuditEntityType.OWNER, owner.getUuid(),
                        "У владельца " + owner.getName() + " удалён тег «" + tag + "»");
            };
        };
    }
}

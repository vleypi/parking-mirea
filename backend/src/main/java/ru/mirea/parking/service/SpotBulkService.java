package ru.mirea.parking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.parking.domain.AuditEntityType;
import ru.mirea.parking.domain.AuditEventType;
import ru.mirea.parking.domain.Spot;
import ru.mirea.parking.dto.BulkResponse;
import ru.mirea.parking.dto.request.BulkAction;
import ru.mirea.parking.dto.spot.SpotBulkRequest;
import ru.mirea.parking.error.BadRequestException;
import ru.mirea.parking.repository.SpotRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Массовые операции над местами: каждая запись в своей транзакции; место с незавершённой заявкой при архивировании попадает в failed. */
@Service
public class SpotBulkService {

    private final SpotService spotService;
    private final SpotRepository spots;
    private final AuditService audit;
    private final TransactionTemplate transaction;

    public SpotBulkService(SpotService spotService, SpotRepository spots, AuditService audit,
                              PlatformTransactionManager transactionManager) {
        this.spotService = spotService;
        this.spots = spots;
        this.audit = audit;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public BulkResponse apply(SpotBulkRequest body) {
        Consumer<Spot> action = toAction(body);
        List<UUID> succeeded = new ArrayList<>();
        List<BulkResponse.Failure> failed = new ArrayList<>();

        for (UUID id : new LinkedHashSet<>(body.ids())) {
            try {
                transaction.executeWithoutResult(status -> {
                    Spot spot = spotService.findSpot(id);
                    action.accept(spot);
                    spots.saveAndFlush(spot);
                });
                succeeded.add(id);
            } catch (ResponseStatusException e) {
                failed.add(new BulkResponse.Failure(id, e.getReason()));
            }
        }
        return new BulkResponse(succeeded, failed);
    }

    private Consumer<Spot> toAction(SpotBulkRequest body) {
        if (body.action() == BulkAction.CHANGE_STATUS && body.status() == null) {
            throw new BadRequestException("status", "Для смены статуса укажите status: AVAILABLE, MAINTENANCE или CLOSED");
        }
        boolean tagAction = body.action() == BulkAction.ADD_TAG || body.action() == BulkAction.REMOVE_TAG;
        if (tagAction && (body.tag() == null || body.tag().isBlank())) {
            throw new BadRequestException("tag", "Для действия с тегом укажите tag");
        }
        return switch (body.action()) {
            case ARCHIVE -> spotService::archive;
            case RESTORE -> spotService::restore;
            case CHANGE_STATUS -> spot -> {
                spotService.checkNotArchived(spot);
                spot.setStatus(body.status());
                audit.record(AuditEventType.UPDATE, AuditEntityType.SPOT, spot.getUuid(),
                        "Месту " + spot.getSpotNumber() + " установлен статус «" + body.status().getTitle() + "»");
            };
            case ADD_TAG -> spot -> {
                spotService.checkNotArchived(spot);
                String tag = SpotService.normalizeTag(body.tag());
                spot.getTags().add(tag);
                audit.record(AuditEventType.UPDATE, AuditEntityType.SPOT, spot.getUuid(),
                        "Месту " + spot.getSpotNumber() + " добавлен тег «" + tag + "»");
            };
            case REMOVE_TAG -> spot -> {
                spotService.checkNotArchived(spot);
                String tag = SpotService.normalizeTag(body.tag());
                spot.getTags().remove(tag);
                audit.record(AuditEventType.UPDATE, AuditEntityType.SPOT, spot.getUuid(),
                        "У места " + spot.getSpotNumber() + " удалён тег «" + tag + "»");
            };
        };
    }
}

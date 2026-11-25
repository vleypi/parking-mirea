package ru.mirea.parking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.parking.domain.AuditEntityType;
import ru.mirea.parking.domain.AuditEventType;
import ru.mirea.parking.domain.Request;
import ru.mirea.parking.dto.request.BulkAction;
import ru.mirea.parking.dto.request.RequestBulkRequest;
import ru.mirea.parking.dto.request.RequestBulkResponse;
import ru.mirea.parking.repository.RequestRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class RequestBulkService {

    private static final Set<BulkAction> TAG_ACTIONS = Set.of(BulkAction.ADD_TAG, BulkAction.REMOVE_TAG);

    private final RequestService requestService;
    private final RequestRepository requests;
    private final AuditService audit;
    private final TransactionTemplate transaction;

    public RequestBulkService(RequestService requestService, RequestRepository requests, AuditService audit,
                              PlatformTransactionManager transactionManager) {
        this.requestService = requestService;
        this.requests = requests;
        this.audit = audit;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public RequestBulkResponse apply(RequestBulkRequest body) {
        Consumer<Request> action = toAction(body);
        List<UUID> succeeded = new ArrayList<>();
        List<RequestBulkResponse.Failure> failed = new ArrayList<>();

        for (UUID id : new LinkedHashSet<>(body.ids())) {
            try {
                transaction.executeWithoutResult(status -> {
                    Request request = requestService.findRequest(id);
                    action.accept(request);
                    requests.saveAndFlush(request);
                });
                succeeded.add(id);
            } catch (ResponseStatusException e) {
                failed.add(new RequestBulkResponse.Failure(id, e.getReason()));
            }
        }
        return new RequestBulkResponse(succeeded, failed);
    }

    private Consumer<Request> toAction(RequestBulkRequest body) {
        if (body.action() == BulkAction.CHANGE_STATUS && isBlank(body.status())) {
            throw new ResponseStatusException(BAD_REQUEST, "Для смены статуса укажите status");
        }
        if (TAG_ACTIONS.contains(body.action()) && isBlank(body.tag())) {
            throw new ResponseStatusException(BAD_REQUEST, "Для действия с тегом укажите tag");
        }
        return switch (body.action()) {
            case ARCHIVE -> requestService::archive;
            case RESTORE -> requestService::restore;
            case CHANGE_STATUS -> request -> {
                requestService.checkNotArchived(request);
                requestService.checkActive(request);
                request.setStatus(requestService.resolveTransition(request, body.status()));
                audit.record(AuditEventType.UPDATE, AuditEntityType.REQUEST, request.getUuid(),
                        "Заявке " + request.getCode() + " установлен статус «" + request.getStatus().getTitle() + "»");
            };
            case ADD_TAG -> request -> {
                requestService.checkNotArchived(request);
                String tag = RequestService.normalizeTag(body.tag());
                request.getTags().add(tag);
                audit.record(AuditEventType.UPDATE, AuditEntityType.REQUEST, request.getUuid(),
                        "Заявке " + request.getCode() + " добавлен тег «" + tag + "»");
            };
            case REMOVE_TAG -> request -> {
                requestService.checkNotArchived(request);
                String tag = RequestService.normalizeTag(body.tag());
                request.getTags().remove(tag);
                audit.record(AuditEventType.UPDATE, AuditEntityType.REQUEST, request.getUuid(),
                        "У заявки " + request.getCode() + " удалён тег «" + tag + "»");
            };
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

package ru.mirea.parking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.parking.domain.AuditEntityType;
import ru.mirea.parking.domain.AuditEvent;
import ru.mirea.parking.domain.AuditEventType;
import ru.mirea.parking.domain.UserAccount;
import ru.mirea.parking.dto.PageResponse;
import ru.mirea.parking.dto.audit.AuditEventResponse;
import ru.mirea.parking.filter.AuditFilter;
import ru.mirea.parking.filter.AuditSpecifications;
import ru.mirea.parking.filter.PageRequestFactory;
import ru.mirea.parking.logging.CorrelationId;
import ru.mirea.parking.logging.CorrelationIdFilter;
import ru.mirea.parking.repository.AuditEventRepository;
import ru.mirea.parking.security.UserPrincipal;

import java.time.Clock;
import java.util.Set;
import java.util.UUID;

/**
 * Единая точка записи в журнал аудита. Сервисы всех сущностей вызывают {@link #record}.
 * <p>
 * Запись идёт в той же транзакции, что и изменение: если изменение откатилось, события в журнале тоже нет.
 * Автор берётся из SecurityContext, correlation id из MDC (его кладёт {@link CorrelationIdFilter}).
 */
@Service
public class AuditService {

    public static final Set<String> SORT_FIELDS = Set.of("occurredAt", "eventType", "actorLogin");

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final int SUMMARY_MAX = 500;
    private static final int LOGIN_MAX = 50;

    private final AuditEventRepository events;
    private final Clock clock;

    public AuditService(AuditEventRepository events, Clock clock) {
        this.events = events;
        this.clock = clock;
    }

    /**
     * Записать действие текущего пользователя над сущностью.
     *
     * @param type       что произошло
     * @param entityType с какой сущностью
     * @param entityId   UUID сущности (внешний идентификатор из API)
     * @param summary    краткое описание для человека, например «Заявка REQ-000007 перенесена в архив»
     */
    @Transactional
    public AuditEvent record(AuditEventType type, AuditEntityType entityType, UUID entityId, String summary) {
        UserPrincipal actor = currentPrincipal();
        return save(new AuditEvent(actor == null ? null : actor.id(), actor == null ? null : actor.login(),
                type, entityType, entityId, truncate(summary, SUMMARY_MAX), CorrelationId.current()));
    }

    /** Успешный вход: пользователь ещё не в SecurityContext, поэтому передаётся явно. */
    @Transactional
    public AuditEvent recordLoginSuccess(UserAccount user) {
        return save(new AuditEvent(user.getId(), user.getLogin(), AuditEventType.LOGIN_SUCCESS,
                AuditEntityType.USER_ACCOUNT, user.getId(), "Вход в систему", CorrelationId.current()));
    }

    /**
     * Неуспешный вход. Логин сохраняется как ввели (обрезанный), учётная запись может и не существовать.
     *
     * @param user   найденная учётная запись или null, если логин неизвестен
     * @param login  логин, который ввели в форму
     * @param reason причина отказа: неверный пароль, блокировка, учётная запись отключена
     */
    @Transactional
    public AuditEvent recordLoginFailure(UserAccount user, String login, String reason) {
        UUID userId = user == null ? null : user.getId();
        return save(new AuditEvent(userId, truncate(login, LOGIN_MAX), AuditEventType.LOGIN_FAILURE,
                AuditEntityType.USER_ACCOUNT, userId, truncate("Неуспешный вход: " + reason, SUMMARY_MAX),
                CorrelationId.current()));
    }

    /** Поиск по журналу для GET /api/admin/audit: фильтры, сортировка и страница. */
    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> search(AuditFilter filter, int page, int size, String sort, String direction) {
        Pageable pageable = PageRequestFactory.create(page, size, sort, direction, SORT_FIELDS);
        return PageResponse.from(events.findAll(AuditSpecifications.byFilter(filter, clock.getZone()), pageable), AuditEventResponse::from);
    }

    private AuditEvent save(AuditEvent event) {
        AuditEvent saved = events.save(event);
        log.info("audit {} {} {} by {}: {}", event.getEventType(), event.getEntityType(), event.getEntityId(),
                event.getActorLogin(), event.getSummary());
        return saved;
    }

    private static UserPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof UserPrincipal principal ? principal : null;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}

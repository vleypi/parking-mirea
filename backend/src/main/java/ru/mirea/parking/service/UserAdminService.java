package ru.mirea.parking.service;

import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.parking.domain.Role;
import ru.mirea.parking.domain.UserAccount;
import ru.mirea.parking.dto.PageResponse;
import ru.mirea.parking.dto.user.TemporaryPasswordResponse;
import ru.mirea.parking.dto.user.UserAccountResponse;
import ru.mirea.parking.dto.user.UserCreateRequest;
import ru.mirea.parking.dto.user.UserUpdateRequest;
import ru.mirea.parking.filter.PageRequestFactory;
import ru.mirea.parking.filter.UserAccountSpecifications;
import ru.mirea.parking.repository.RefreshSessionRepository;
import ru.mirea.parking.repository.UserAccountRepository;
import ru.mirea.parking.security.TemporaryPasswordGenerator;
import ru.mirea.parking.security.UserPrincipal;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class UserAdminService {

    public static final Set<String> SORT_FIELDS = Set.of("login", "displayName", "createdAt");

    private static final String STALE_VERSION = "Запись изменил другой пользователь, обновите карточку";

    private final UserAccountRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator passwordGenerator;

    public UserAdminService(UserAccountRepository users, RefreshSessionRepository sessions,
                            PasswordEncoder passwordEncoder, TemporaryPasswordGenerator passwordGenerator) {
        this.users = users;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.passwordGenerator = passwordGenerator;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserAccountResponse> search(String q, int page, int size, String sort, String direction) {
        Pageable pageable = PageRequestFactory.create(page, size, sort, direction, SORT_FIELDS);
        return PageResponse.from(users.findAll(UserAccountSpecifications.textSearch(q), pageable), UserAccountResponse::from);
    }

    @Transactional
    public TemporaryPasswordResponse create(UserCreateRequest body) {
        String login = AuthService.normalizeLogin(body.login());
        if (users.existsByLogin(login)) {
            throw new ResponseStatusException(CONFLICT, "Логин «" + login + "» уже занят");
        }
        String temporaryPassword = passwordGenerator.generate();
        UserAccount user = new UserAccount(login, body.displayName().trim(), passwordEncoder.encode(temporaryPassword), body.roles());
        return new TemporaryPasswordResponse(UserAccountResponse.from(users.saveAndFlush(user)), temporaryPassword);
    }

    @Transactional
    public UserAccountResponse update(UUID id, UserUpdateRequest body, UserPrincipal admin) {
        UserAccount user = findUser(id);
        if (body.version() != user.getVersion()) {
            throw new ResponseStatusException(CONFLICT, STALE_VERSION);
        }
        String login = AuthService.normalizeLogin(body.login());
        if (users.existsByLoginAndIdNot(login, id)) {
            throw new ResponseStatusException(CONFLICT, "Логин «" + login + "» уже занят");
        }
        if (user.getId().equals(admin.id())) {
            checkSelfUpdate(body);
        }

        boolean losesAccess = user.isEnabled() && !body.enabled();
        user.setLogin(login);
        user.setDisplayName(body.displayName().trim());
        user.setRoles(body.roles());
        user.setEnabled(body.enabled());
        try {
            users.saveAndFlush(user);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ResponseStatusException(CONFLICT, STALE_VERSION);
        }
        if (losesAccess) {
            sessions.revokeAllForUser(user.getId(), Instant.now());
        }
        return UserAccountResponse.from(user);
    }

    @Transactional
    public TemporaryPasswordResponse resetPassword(UUID id) {
        UserAccount user = findUser(id);
        String temporaryPassword = passwordGenerator.generate();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        users.saveAndFlush(user);
        sessions.revokeAllForUser(user.getId(), Instant.now());
        return new TemporaryPasswordResponse(UserAccountResponse.from(user), temporaryPassword);
    }

    private void checkSelfUpdate(UserUpdateRequest body) {
        if (!body.enabled()) {
            throw new ResponseStatusException(CONFLICT, "Нельзя отключить собственную учётную запись");
        }
        if (!body.roles().contains(Role.ADMIN)) {
            throw new ResponseStatusException(CONFLICT, "Нельзя снять с себя роль администратора");
        }
    }

    private UserAccount findUser(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Пользователь " + id + " не найден"));
    }
}

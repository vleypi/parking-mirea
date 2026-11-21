package ru.mirea.parking.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.parking.config.AppProperties;
import ru.mirea.parking.domain.RefreshSession;
import ru.mirea.parking.domain.UserAccount;
import ru.mirea.parking.repository.RefreshSessionRepository;
import ru.mirea.parking.repository.UserAccountRepository;
import ru.mirea.parking.security.AuthMessages;
import ru.mirea.parking.security.JwtService;
import ru.mirea.parking.security.PasswordPolicy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    public record IssuedTokens(String accessToken, Duration accessTtl, String refreshToken, Duration refreshTtl) {
    }

    public record AuthResult(UserAccount user, IssuedTokens tokens) {
    }

    private final UserAccountRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final JwtService jwtService;
    private final AuditService audit;
    private final Duration refreshTtl;
    private final String dummyHash;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserAccountRepository users, RefreshSessionRepository sessions,
                       PasswordEncoder passwordEncoder, PasswordPolicy passwordPolicy,
                       JwtService jwtService, AuditService audit, AppProperties properties) {
        this.users = users;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.jwtService = jwtService;
        this.audit = audit;
        this.refreshTtl = Duration.ofDays(properties.jwt().refreshDays());
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing-1");
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public AuthResult login(String rawLogin, String password, String userAgent, String ip) {
        UserAccount user = users.findByLogin(normalizeLogin(rawLogin)).orElse(null);
        boolean passwordMatches = passwordEncoder.matches(password, user == null ? dummyHash : user.getPasswordHash());
        Instant now = Instant.now();

        if (user == null) {
            audit.recordLoginFailure(null, normalizeLogin(rawLogin), "неизвестный логин");
            throw new ResponseStatusException(UNAUTHORIZED, AuthMessages.INVALID_CREDENTIALS);
        }
        if (user.isLockedAt(now)) {
            audit.recordLoginFailure(user, user.getLogin(), "вход временно заблокирован");
            throw new ResponseStatusException(TOO_MANY_REQUESTS, AuthMessages.LOGIN_LOCKED);
        }
        if (!passwordMatches) {
            registerFailedAttempt(user, now);
            audit.recordLoginFailure(user, user.getLogin(), "неверный пароль");
            throw new ResponseStatusException(UNAUTHORIZED, AuthMessages.INVALID_CREDENTIALS);
        }
        if (!user.isEnabled()) {
            audit.recordLoginFailure(user, user.getLogin(), "учётная запись отключена");
            throw new ResponseStatusException(FORBIDDEN, AuthMessages.ACCOUNT_DISABLED);
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        audit.recordLoginSuccess(user);
        return new AuthResult(user, issueTokens(user, userAgent, ip, now));
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public AuthResult refresh(String rawRefreshToken, String userAgent, String ip) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank() || rawRefreshToken.length() > 200) {
            throw new ResponseStatusException(UNAUTHORIZED, AuthMessages.SESSION_EXPIRED);
        }
        RefreshSession current = sessions.findByTokenHash(sha256(rawRefreshToken))
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, AuthMessages.SESSION_EXPIRED));
        Instant now = Instant.now();
        UserAccount user = current.getUser();

        if (current.getRevokedAt() != null) {
            sessions.revokeAllForUser(user.getId(), now);
            throw new ResponseStatusException(UNAUTHORIZED, AuthMessages.SESSION_EXPIRED);
        }
        current.revoke(now);
        if (!current.getExpiresAt().isAfter(now) || !user.isEnabled()) {
            throw new ResponseStatusException(UNAUTHORIZED, AuthMessages.SESSION_EXPIRED);
        }
        return new AuthResult(user, issueTokens(user, userAgent, ip, now));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank() || rawRefreshToken.length() > 200) {
            return;
        }
        sessions.findByTokenHash(sha256(rawRefreshToken)).ifPresent(session -> session.revoke(Instant.now()));
    }

    @Transactional(readOnly = true)
    public UserAccount me(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, AuthMessages.LOGIN_REQUIRED));
    }

    @Transactional
    public IssuedTokens changePassword(UUID userId, String currentPassword, String newPassword,
                                       String userAgent, String ip) {
        UserAccount user = me(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(BAD_REQUEST, AuthMessages.WRONG_CURRENT_PASSWORD);
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(BAD_REQUEST, AuthMessages.SAME_PASSWORD);
        }
        List<String> violations = passwordPolicy.violations(newPassword, user.getLogin());
        if (!violations.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, String.join("; ", violations));
        }

        Instant now = Instant.now();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        users.saveAndFlush(user);
        sessions.revokeAllForUser(user.getId(), now);
        return issueTokens(user, userAgent, ip, now);
    }

    public static String normalizeLogin(String login) {
        return login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
    }

    private void registerFailedAttempt(UserAccount user, Instant now) {
        int attempts = user.getFailedLoginAttempts() + 1;
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(now.plus(LOCK_DURATION));
            attempts = 0;
        }
        user.setFailedLoginAttempts(attempts);
    }

    private IssuedTokens issueTokens(UserAccount user, String userAgent, String ip, Instant now) {
        String refreshToken = randomToken();
        sessions.save(new RefreshSession(user, sha256(refreshToken), now.plus(refreshTtl),
                truncate(userAgent, 500), truncate(ip, 64)));
        return new IssuedTokens(jwtService.issue(user), jwtService.accessTtl(), refreshToken, refreshTtl);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}

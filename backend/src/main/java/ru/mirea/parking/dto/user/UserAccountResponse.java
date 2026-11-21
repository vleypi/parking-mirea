package ru.mirea.parking.dto.user;

import ru.mirea.parking.domain.Role;
import ru.mirea.parking.domain.UserAccount;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserAccountResponse(
        UUID id,
        String login,
        String displayName,
        Set<Role> roles,
        boolean enabled,
        boolean mustChangePassword,
        Instant lockedUntil,
        Instant createdAt,
        long version) {

    public static UserAccountResponse from(UserAccount user) {
        return new UserAccountResponse(user.getId(), user.getLogin(), user.getDisplayName(), Set.copyOf(user.getRoles()),
                user.isEnabled(), user.isMustChangePassword(), user.getLockedUntil(), user.getCreatedAt(), user.getVersion());
    }
}

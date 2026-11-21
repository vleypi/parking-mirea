package ru.mirea.parking.dto.auth;

import ru.mirea.parking.domain.Role;
import ru.mirea.parking.domain.UserAccount;

import java.util.Set;
import java.util.UUID;

public record MeResponse(UUID id, String login, String displayName, Set<Role> roles, boolean mustChangePassword) {

    public static MeResponse from(UserAccount user) {
        return new MeResponse(user.getId(), user.getLogin(), user.getDisplayName(),
                Set.copyOf(user.getRoles()), user.isMustChangePassword());
    }
}

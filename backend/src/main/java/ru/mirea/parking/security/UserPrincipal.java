package ru.mirea.parking.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import ru.mirea.parking.domain.Role;
import ru.mirea.parking.domain.UserAccount;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record UserPrincipal(UUID id, String login, String displayName,
                            Set<Role> roles, boolean mustChangePassword) {

    public static UserPrincipal from(UserAccount user) {
        return new UserPrincipal(user.getId(), user.getLogin(), user.getDisplayName(),
                Set.copyOf(user.getRoles()), user.isMustChangePassword());
    }

    public List<GrantedAuthority> authorities() {
        return roles.stream()
                .<GrantedAuthority>map(role -> new SimpleGrantedAuthority(role.authority()))
                .toList();
    }
}

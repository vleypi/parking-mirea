package ru.mirea.parking.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import ru.mirea.parking.domain.Role;

import java.util.Set;

public record UserUpdateRequest(
        @NotBlank @Size(max = 50) @Pattern(regexp = UserLogin.PATTERN, message = UserLogin.MESSAGE) String login,
        @NotBlank @Size(max = 100) String displayName,
        @NotEmpty Set<Role> roles,
        @NotNull Boolean enabled,
        @NotNull @PositiveOrZero Long version) {
}

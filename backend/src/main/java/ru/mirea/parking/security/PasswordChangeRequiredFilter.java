package ru.mirea.parking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED = Set.of(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/me",
            "/api/auth/session",
            "/api/auth/change-password");

    private final JsonSecurityErrorHandler errors;

    public PasswordChangeRequiredFilter(JsonSecurityErrorHandler errors) {
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal
                && principal.mustChangePassword()
                && !ALLOWED.contains(request.getRequestURI())) {
            errors.write(request, response, HttpStatus.FORBIDDEN, AuthMessages.PASSWORD_CHANGE_REQUIRED);
            return;
        }
        chain.doFilter(request, response);
    }
}

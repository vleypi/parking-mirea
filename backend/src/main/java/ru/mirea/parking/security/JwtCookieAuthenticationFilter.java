package ru.mirea.parking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.mirea.parking.domain.UserAccount;
import ru.mirea.parking.repository.UserAccountRepository;

import java.io.IOException;

public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthCookies cookies;
    private final UserAccountRepository users;

    public JwtCookieAuthenticationFilter(JwtService jwtService, AuthCookies cookies, UserAccountRepository users) {
        this.jwtService = jwtService;
        this.cookies = cookies;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.parseUserId(token)
                    .flatMap(users::findById)
                    .filter(UserAccount::isEnabled)
                    .map(UserPrincipal::from)
                    .ifPresent(principal -> {
                        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                                principal, null, principal.authorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        return cookies.read(request, AuthCookies.ACCESS);
    }
}

package ru.mirea.parking.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.mirea.parking.error.ErrorCodes;
import ru.mirea.parking.error.ErrorResponse;
import ru.mirea.parking.dto.auth.ChangePasswordRequest;
import ru.mirea.parking.dto.auth.LoginRequest;
import ru.mirea.parking.dto.auth.MeResponse;
import ru.mirea.parking.dto.auth.SessionResponse;
import ru.mirea.parking.security.AuthCookies;
import ru.mirea.parking.security.UserPrincipal;
import ru.mirea.parking.service.AuthService;
import ru.mirea.parking.service.AuthService.AuthResult;
import ru.mirea.parking.service.AuthService.IssuedTokens;


@Tag(name = "auth")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookies cookies;

    public AuthController(AuthService authService, AuthCookies cookies) {
        this.authService = authService;
        this.cookies = cookies;
    }

    @PostMapping("/login")
    public MeResponse login(@Valid @RequestBody LoginRequest body,
                            HttpServletRequest request, HttpServletResponse response) {
        AuthResult result = authService.login(body.login(), body.password(), userAgent(request), request.getRemoteAddr());
        writeCookies(response, result.tokens());
        return MeResponse.from(result.user());
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        try {
            AuthResult result = authService.refresh(cookies.read(request, AuthCookies.REFRESH),
                    userAgent(request), request.getRemoteAddr());
            writeCookies(response, result.tokens());
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException e) {
            cookies.clear(response);
            int status = e.getStatusCode().value();
            return ResponseEntity.status(status).body(ErrorResponse.of(status, ErrorCodes.forStatus(status),
                    e.getReason(), request.getRequestURI()));
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(cookies.read(request, AuthCookies.REFRESH));
        cookies.clear(response);
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return MeResponse.from(authService.me(principal.id()));
    }

    @GetMapping("/session")
    public SessionResponse session() {
        return new SessionResponse(true);
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal UserPrincipal principal,
                               @Valid @RequestBody ChangePasswordRequest body,
                               HttpServletRequest request, HttpServletResponse response) {
        IssuedTokens tokens = authService.changePassword(principal.id(), body.currentPassword(), body.newPassword(),
                userAgent(request), request.getRemoteAddr());
        writeCookies(response, tokens);
    }

    private void writeCookies(HttpServletResponse response, IssuedTokens tokens) {
        cookies.write(response, tokens.accessToken(), tokens.accessTtl(), tokens.refreshToken(), tokens.refreshTtl());
    }

    private String userAgent(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.USER_AGENT);
    }
}

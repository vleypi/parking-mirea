package ru.mirea.parking.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import ru.mirea.parking.config.AppProperties;

import java.time.Duration;

@Component
public class AuthCookies {

    public static final String ACCESS = "parking_access";
    public static final String REFRESH = "parking_refresh";

    private final boolean secure;

    public AuthCookies(AppProperties properties) {
        this.secure = properties.cookieSecure();
    }

    public void write(HttpServletResponse response, String accessToken, Duration accessTtl,
                      String refreshToken, Duration refreshTtl) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(ACCESS, accessToken, accessTtl).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, build(REFRESH, refreshToken, refreshTtl).toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, build(ACCESS, "", Duration.ZERO).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, build(REFRESH, "", Duration.ZERO).toString());
    }

    public String read(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private ResponseCookie build(String name, String value, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}

package ru.mirea.parking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Bootstrap bootstrap, Cors cors,
                            boolean cookieSecure, boolean permanentDeleteEnabled) {

    public record Jwt(String secretBase64, String issuer, int accessMinutes, int refreshDays) {
    }

    public record Bootstrap(String login, String name, String password) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}

package ru.mirea.parking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import ru.mirea.parking.config.AppProperties;
import ru.mirea.parking.domain.UserAccount;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTtl;

    public JwtService(AppProperties properties) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.jwt().secretBase64()));
        this.issuer = properties.jwt().issuer();
        this.accessTtl = Duration.ofMinutes(properties.jwt().accessMinutes());
    }

    public Duration accessTtl() {
        return accessTtl;
    }

    public String issue(UserAccount user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key)
                .compact();
    }

    public Optional<UUID> parseUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(UUID.fromString(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

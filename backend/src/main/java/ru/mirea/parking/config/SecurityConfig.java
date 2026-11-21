package ru.mirea.parking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ВРЕМЕННАЯ настройка безопасности каркаса: все запросы разрешены.
 *
 * <p>Без этого класса Spring Security закрыл бы все адреса и сгенерировал случайный пароль,
 * и никто не смог бы проверить свои эндпоинты в Swagger. Класс полностью заменяется
 * в задаче «Вход, JWT и refresh-сессии» (роли VIEWER / OPERATOR / ADMIN).</p>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST API без cookie-сессий, поэтому CSRF-защита формы не нужна
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}

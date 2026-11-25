package ru.mirea.parking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.mirea.parking.security.AuthCookies;

/** Документация REST API: схемы авторизации и группы эндпоинтов по сущностям в Swagger UI. */
@Configuration
public class OpenApiConfig {

    /** Access JWT в HttpOnly-cookie: Swagger на том же адресе отправляет её сам после входа. */
    public static final String COOKIE_AUTH = "cookieAuth";

    @Bean
    public OpenAPI parkingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Parking REST API")
                        .version("1.0.0")
                        .description("""
                                Парковочная система: заявки, владельцы, автомобили, места, справочники, аналитика.

                                Авторизация: войдите через /swagger-login.html (или POST /api/auth/login) — \
                                access-токен ляжет в cookie и будет отправляться со всеми запросами из Swagger. \
                                Чтение доступно всем ролям, изменение — Оператору и Администратору, \
                                окончательное удаление и администрирование — Администратору."""))
                .components(new Components()
                        .addSecuritySchemes(COOKIE_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.COOKIE).name(AuthCookies.ACCESS)))
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_AUTH));
    }

    @Bean
    public GroupedOpenApi allApi() {
        return group("all", "/api/**");
    }

    @Bean
    public GroupedOpenApi requestsApi() {
        return group("requests", "/api/requests/**");
    }

    @Bean
    public GroupedOpenApi ownersApi() {
        return group("owners", "/api/owners/**");
    }

    @Bean
    public GroupedOpenApi vehiclesApi() {
        return group("vehicles", "/api/vehicles/**");
    }

    @Bean
    public GroupedOpenApi spotsApi() {
        return group("spots", "/api/spots/**");
    }

    @Bean
    public GroupedOpenApi referencesAndAnalyticsApi() {
        return group("references-analytics", "/api/references/**", "/api/analytics/**");
    }

    @Bean
    public GroupedOpenApi authAndAdminApi() {
        return group("auth-admin", "/api/auth/**", "/api/admin/**");
    }

    private static GroupedOpenApi group(String name, String... paths) {
        return GroupedOpenApi.builder().group(name).pathsToMatch(paths).build();
    }
}

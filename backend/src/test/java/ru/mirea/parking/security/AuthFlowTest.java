package ru.mirea.parking.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.mirea.parking.domain.Role;
import ru.mirea.parking.domain.UserAccount;
import ru.mirea.parking.repository.RefreshSessionRepository;
import ru.mirea.parking.repository.UserAccountRepository;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.bootstrap.login=Admin",
        "app.bootstrap.password=AdminPass2026",
        "app.bootstrap.name=Администратор"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest {

    private static final String PASSWORD = "Password2026";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserAccountRepository users;
    @Autowired
    private RefreshSessionRepository sessions;
    @Autowired
    private PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        sessions.deleteAll();
        users.findAll().stream().filter(u -> !u.getLogin().equals("admin")).forEach(users::delete);
        createUser("viewer", Role.VIEWER, false);
        createUser("operator", Role.OPERATOR, false);
        createUser("newbie", Role.OPERATOR, true);
    }

    @Test
    void bootstrapAdminCreatedAndCanLogin() throws Exception {
        MvcResult result = login("admin", "AdminPass2026")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("token", "password", "Hash");

        Cookie access = result.getResponse().getCookie(AuthCookies.ACCESS);
        Cookie refresh = result.getResponse().getCookie(AuthCookies.REFRESH);
        assertThat(access).isNotNull();
        assertThat(access.isHttpOnly()).isTrue();
        assertThat(refresh).isNotNull();
        assertThat(refresh.isHttpOnly()).isTrue();
        assertThat(result.getResponse().getHeaders("Set-Cookie")).allMatch(h -> h.contains("SameSite=Lax"));

        mvc.perform(get("/api/auth/me").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("admin"));
        mvc.perform(get("/api/auth/session").cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true));
    }

    @Test
    void withoutCookieIs401() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(AuthMessages.LOGIN_REQUIRED));
        mvc.perform(get("/api/auth/me").cookie(new Cookie(AuthCookies.ACCESS, "garbage")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordIs401() throws Exception {
        login("viewer", "WrongPass2026")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(AuthMessages.INVALID_CREDENTIALS));
        login("nobody", PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTokensAndOldOneStopsWorking() throws Exception {
        MvcResult loginResult = login("operator", PASSWORD).andExpect(status().isOk()).andReturn();
        Cookie oldRefresh = loginResult.getResponse().getCookie(AuthCookies.REFRESH);

        MvcResult refreshed = mvc.perform(post("/api/auth/refresh").cookie(oldRefresh))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie newAccess = refreshed.getResponse().getCookie(AuthCookies.ACCESS);
        Cookie newRefresh = refreshed.getResponse().getCookie(AuthCookies.REFRESH);
        assertThat(newRefresh.getValue()).isNotEqualTo(oldRefresh.getValue());
        mvc.perform(get("/api/auth/me").cookie(newAccess)).andExpect(status().isOk());

        MvcResult reused = mvc.perform(post("/api/auth/refresh").cookie(oldRefresh))
                .andExpect(status().isUnauthorized())
                .andReturn();
        assertThat(reused.getResponse().getCookie(AuthCookies.REFRESH).getMaxAge()).isZero();
        mvc.perform(post("/api/auth/refresh").cookie(newRefresh)).andExpect(status().isUnauthorized());
    }

    @Test
    void afterLogoutRefreshFails() throws Exception {
        Cookie refresh = login("operator", PASSWORD).andReturn().getResponse().getCookie(AuthCookies.REFRESH);

        MvcResult logout = mvc.perform(post("/api/auth/logout").cookie(refresh))
                .andExpect(status().isNoContent())
                .andReturn();
        assertThat(logout.getResponse().getCookie(AuthCookies.ACCESS).getMaxAge()).isZero();

        mvc.perform(post("/api/auth/refresh").cookie(refresh)).andExpect(status().isUnauthorized());
    }

    @Test
    void sixthWrongAttemptIsBlockedEvenWithCorrectPassword() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("viewer", "WrongPass2026").andExpect(status().isUnauthorized());
        }
        login("viewer", PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(AuthMessages.LOGIN_LOCKED));
        assertThat(users.findByLogin("viewer").orElseThrow().getLockedUntil()).isNotNull();
    }

    @Test
    void disabledAccountCannotLogin() throws Exception {
        UserAccount viewer = users.findByLogin("viewer").orElseThrow();
        viewer.setEnabled(false);
        users.save(viewer);
        login("viewer", PASSWORD).andExpect(status().isForbidden());
    }

    @Test
    void rolesAreCheckedByHttpMethod() throws Exception {
        Cookie viewer = accessCookie("viewer");
        Cookie operator = accessCookie("operator");
        Cookie admin = accessCookie("admin");

        mvc.perform(get("/api/anything").cookie(viewer)).andExpect(status().isNotFound());
        mvc.perform(post("/api/anything").cookie(viewer)).andExpect(status().isForbidden());
        mvc.perform(post("/api/anything").cookie(operator)).andExpect(status().isNotFound());
        mvc.perform(post("/api/anything").cookie(admin)).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/users").cookie(operator)).andExpect(status().isForbidden());
    }

    @Test
    void swaggerOnlyForAdmin() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/swagger-login.html"));
        mvc.perform(get("/v3/api-docs").cookie(accessCookie("operator"))).andExpect(status().isForbidden());
        mvc.perform(get("/v3/api-docs").cookie(accessCookie("admin"))).andExpect(status().isOk());
        mvc.perform(get("/swagger-login.html")).andExpect(status().isOk());
    }

    @Test
    void temporaryPasswordAllowsOnlyPasswordChange() throws Exception {
        Cookie access = accessCookie("newbie");
        mvc.perform(get("/api/anything").cookie(access))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(AuthMessages.PASSWORD_CHANGE_REQUIRED));
        mvc.perform(get("/api/auth/me").cookie(access))
                .andExpect(jsonPath("$.mustChangePassword").value(true));

        mvc.perform(post("/api/auth/change-password").cookie(access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"short1\"}"))
                .andExpect(status().isBadRequest());

        MvcResult changed = mvc.perform(post("/api/auth/change-password").cookie(access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"NewPassword2026\"}"))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie newAccess = changed.getResponse().getCookie(AuthCookies.ACCESS);

        mvc.perform(get("/api/anything").cookie(newAccess)).andExpect(status().isNotFound());
        login("newbie", PASSWORD).andExpect(status().isUnauthorized());
        login("newbie", "NewPassword2026").andExpect(status().isOk());
    }

    @Test
    void foreignOriginIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").header("Origin", "http://evil.example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"viewer\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").header("Origin", "http://localhost:3000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"viewer\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions login(String login, String password) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}"));
    }

    private Cookie accessCookie(String login) throws Exception {
        String password = login.equals("admin") ? "AdminPass2026" : PASSWORD;
        return login(login, password).andReturn().getResponse().getCookie(AuthCookies.ACCESS);
    }

    private void createUser(String login, Role role, boolean mustChangePassword) {
        UserAccount user = new UserAccount(login, login, encoder.encode(PASSWORD), Set.of(role));
        user.setMustChangePassword(mustChangePassword);
        users.save(user);
    }
}

package ru.mirea.parking.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import ru.mirea.parking.repository.RefreshSessionRepository;
import ru.mirea.parking.repository.UserAccountRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.bootstrap.login=admin",
        "app.bootstrap.password=AdminPass2026",
        "app.bootstrap.name=Администратор"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserTest {

    private static final String ADMIN_PASSWORD = "AdminPass2026";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private UserAccountRepository users;
    @Autowired
    private RefreshSessionRepository sessions;

    private Cookie admin;

    @BeforeEach
    void setUp() throws Exception {
        sessions.deleteAll();
        users.findAll().stream().filter(u -> !u.getLogin().equals("admin")).forEach(users::delete);
        admin = accessCookie(login("admin", ADMIN_PASSWORD).andExpect(status().isOk()));
    }

    @Test
    void createdOperatorMustChangeTemporaryPassword() throws Exception {
        JsonNode created = body(create("Oper.Ivanov", "Иван Оператор", "OPERATOR").andExpect(status().isCreated()));
        String temporaryPassword = created.get("temporaryPassword").asText();
        assertThat(created.get("user").get("login").asText()).isEqualTo("oper.ivanov");
        assertThat(created.get("user").get("mustChangePassword").asBoolean()).isTrue();
        assertThat(temporaryPassword).hasSize(12).matches(".*\\d.*").matches(".*[A-Za-z].*");

        Cookie operator = accessCookie(login("oper.ivanov", temporaryPassword)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true)));
        mvc.perform(get("/api/requests").cookie(operator)).andExpect(status().isForbidden());

        Cookie changed = accessCookie(mvc.perform(post("/api/auth/change-password").cookie(operator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("currentPassword", temporaryPassword, "newPassword", "Operator2026"))))
                .andExpect(status().isNoContent()));
        mvc.perform(get("/api/requests").cookie(changed)).andExpect(status().isOk());
        mvc.perform(get("/api/admin/users").cookie(changed)).andExpect(status().isForbidden());
    }

    @Test
    void searchPagesAndSorts() throws Exception {
        create("viewer.anna", "Анна Наблюдатель", "VIEWER");
        create("oper.boris", "Борис Оператор", "OPERATOR");

        mvc.perform(get("/api/admin/users?q=анна").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].login").value("viewer.anna"))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());
        mvc.perform(get("/api/admin/users?sort=login&direction=asc").cookie(admin))
                .andExpect(jsonPath("$.content[0].login").value("admin"));
        mvc.perform(get("/api/admin/users?sort=password").cookie(admin)).andExpect(status().isBadRequest());
    }

    @Test
    void duplicateLoginIsConflict() throws Exception {
        create("dup", "Первый", "VIEWER").andExpect(status().isCreated());
        create("DUP", "Второй", "VIEWER").andExpect(status().isConflict());
        create("bad login!", "Кто-то", "VIEWER").andExpect(status().isBadRequest());
    }

    @Test
    void disabledUserLosesAccessImmediately() throws Exception {
        JsonNode created = body(create("oper.olga", "Ольга", "OPERATOR"));
        String id = created.get("user").get("id").asText();
        long version = created.get("user").get("version").asLong();
        String password = created.get("temporaryPassword").asText();
        MvcResult loggedIn = login("oper.olga", password).andReturn();
        Cookie access = loggedIn.getResponse().getCookie(AuthCookies.ACCESS);
        Cookie refresh = loggedIn.getResponse().getCookie(AuthCookies.REFRESH);

        update(id, "oper.olga", "Ольга", List.of("OPERATOR"), false, version).andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mvc.perform(get("/api/auth/me").cookie(access)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").cookie(refresh)).andExpect(status().isUnauthorized());
        login("oper.olga", password).andExpect(status().isForbidden());
    }

    @Test
    void updateChecksVersionAndChangesRoles() throws Exception {
        JsonNode petr = body(create("petr", "Пётр", "VIEWER")).get("user");
        String id = petr.get("id").asText();
        long version = petr.get("version").asLong();
        JsonNode updated = body(update(id, "petr", "Пётр Петров", List.of("OPERATOR"), true, version)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("OPERATOR"))
                .andExpect(jsonPath("$.displayName").value("Пётр Петров")));
        assertThat(updated.get("version").asLong()).isGreaterThan(version);
        update(id, "petr", "Пётр", List.of("VIEWER"), true, version).andExpect(status().isConflict());
    }

    @Test
    void adminCannotDisableOrDemoteSelf() throws Exception {
        JsonNode me = body(mvc.perform(get("/api/auth/me").cookie(admin)));
        String id = me.get("id").asText();
        long version = users.findByLogin("admin").orElseThrow().getVersion();

        update(id, "admin", "Администратор", List.of("ADMIN"), false, version)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Нельзя отключить собственную учётную запись"));
        update(id, "admin", "Администратор", List.of("OPERATOR"), true, version)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Нельзя снять с себя роль администратора"));
    }

    @Test
    void resetPasswordIssuesNewTemporaryPasswordAndUnlocks() throws Exception {
        JsonNode created = body(create("ivan", "Иван", "VIEWER"));
        String id = created.get("user").get("id").asText();
        String oldPassword = created.get("temporaryPassword").asText();
        for (int i = 0; i < 6; i++) {
            login("ivan", "WrongPass2026");
        }
        login("ivan", oldPassword).andExpect(status().isTooManyRequests());

        String newPassword = body(mvc.perform(post("/api/admin/users/" + id + "/reset-password").cookie(admin))
                .andExpect(status().isOk())).get("temporaryPassword").asText();

        assertThat(newPassword).isNotEqualTo(oldPassword);
        login("ivan", oldPassword).andExpect(status().isUnauthorized());
        login("ivan", newPassword).andExpect(status().isOk()).andExpect(jsonPath("$.mustChangePassword").value(true));
    }

    @Test
    void nonAdminGets403() throws Exception {
        String password = body(create("oper.x", "Оператор", "OPERATOR")).get("temporaryPassword").asText();
        Cookie operator = accessCookie(login("oper.x", password));
        mvc.perform(get("/api/admin/users").cookie(operator)).andExpect(status().isForbidden());
    }

    private ResultActions create(String login, String name, String role) throws Exception {
        return mvc.perform(post("/api/admin/users").cookie(admin).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("login", login, "displayName", name, "roles", List.of(role)))));
    }

    private ResultActions update(String id, String login, String name, List<String> roles, boolean enabled, long version) throws Exception {
        return mvc.perform(put("/api/admin/users/" + id).cookie(admin).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("login", login, "displayName", name, "roles", roles,
                        "enabled", enabled, "version", version))));
    }

    private ResultActions login(String login, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("login", login, "password", password))));
    }

    private Cookie accessCookie(ResultActions result) {
        return result.andReturn().getResponse().getCookie(AuthCookies.ACCESS);
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
}

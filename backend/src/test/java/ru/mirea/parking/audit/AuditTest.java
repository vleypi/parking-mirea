package ru.mirea.parking.audit;

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
import org.springframework.test.web.servlet.ResultActions;
import ru.mirea.parking.domain.AuditEntityType;
import ru.mirea.parking.domain.AuditEvent;
import ru.mirea.parking.domain.AuditEventType;
import ru.mirea.parking.domain.Role;
import ru.mirea.parking.domain.UserAccount;
import ru.mirea.parking.logging.CorrelationId;
import ru.mirea.parking.repository.AuditEventRepository;
import ru.mirea.parking.repository.RefreshSessionRepository;
import ru.mirea.parking.repository.UserAccountRepository;
import ru.mirea.parking.security.AuthCookies;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditTest {

    private static final String PASSWORD = "Password2026";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AuditEventRepository events;
    @Autowired
    private UserAccountRepository users;
    @Autowired
    private RefreshSessionRepository sessions;
    @Autowired
    private PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        events.deleteAll();
        sessions.deleteAll();
        users.deleteAll();
        createUser("auditor", Role.ADMIN);
        createUser("operator", Role.OPERATOR);
    }

    @Test
    void successfulAndFailedLoginsAreRecorded() throws Exception {
        login("operator", PASSWORD).andExpect(status().isOk());
        login("operator", "wrong-password").andExpect(status().isUnauthorized());
        login("nobody", PASSWORD).andExpect(status().isUnauthorized());

        List<AuditEvent> all = events.findAll();
        assertThat(all).extracting(AuditEvent::getEventType)
                .containsExactlyInAnyOrder(AuditEventType.LOGIN_SUCCESS, AuditEventType.LOGIN_FAILURE, AuditEventType.LOGIN_FAILURE);
        assertThat(all).extracting(AuditEvent::getActorLogin).containsOnly("operator", "nobody");
        assertThat(all).allMatch(e -> e.getEntityType() == AuditEntityType.USER_ACCOUNT);
        assertThat(all).filteredOn(e -> e.getEventType() == AuditEventType.LOGIN_FAILURE)
                .extracting(AuditEvent::getSummary)
                .containsExactlyInAnyOrder("Неуспешный вход: неверный пароль", "Неуспешный вход: неизвестный логин");
    }

    @Test
    void sameCorrelationIdInResponseHeaderAndAuditRecord() throws Exception {
        String cid = "demo-request-42";
        mvc.perform(post("/api/auth/login").header(CorrelationId.HEADER, cid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"operator\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationId.HEADER, cid));

        List<AuditEvent> recorded = events.findByCorrelationId(cid);
        assertThat(recorded).hasSize(1);
        assertThat(recorded.getFirst().getEventType()).isEqualTo(AuditEventType.LOGIN_SUCCESS);
    }

    @Test
    void correlationIdIsGeneratedWhenMissingOrInvalid() throws Exception {
        MvcResult result = mvc.perform(get("/actuator/health"))
                .andExpect(header().exists(CorrelationId.HEADER))
                .andReturn();
        assertThat(result.getResponse().getHeader(CorrelationId.HEADER)).hasSize(36);

        mvc.perform(get("/actuator/health").header(CorrelationId.HEADER, "bad value\r\nX-Evil: 1"))
                .andExpect(header().string(CorrelationId.HEADER, matchesPattern("[0-9a-f-]{36}")));
    }

    @Test
    void correlationIdPresentOnSecurityErrors() throws Exception {
        mvc.perform(get("/api/admin/audit").header(CorrelationId.HEADER, "abc-401"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(CorrelationId.HEADER, "abc-401"));
    }

    @Test
    void adminSearchesAuditWithFilters() throws Exception {
        login("operator", PASSWORD);
        login("operator", "wrong-password");
        Cookie admin = accessCookie("auditor");

        mvc.perform(get("/api/admin/audit").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].actor.login").value("auditor"));

        mvc.perform(get("/api/admin/audit?eventType=login_failure&entityType=USER_ACCOUNT").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].eventType.code").value("LOGIN_FAILURE"))
                .andExpect(jsonPath("$.content[0].eventType.title").value("Неуспешный вход"));

        mvc.perform(get("/api/admin/audit?q=оператор%25_").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(get("/api/admin/audit?q=OPERATOR&sort=actorLogin&direction=asc&size=1000").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.content[*].actor.login", everyItem(is("operator"))));

        mvc.perform(get("/api/admin/audit?sort=summary").cookie(admin)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/audit?from=2030-01-02T00:00&to=2030-01-01T00:00").cookie(admin))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/audit?from=2000-01-01T00:00&to=2001-01-01T00:00:00").cookie(admin))
                .andExpect(jsonPath("$.totalElements").value(0));

        mvc.perform(get("/api/admin/audit/types").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventTypes[0].code").value("LOGIN_SUCCESS"))
                .andExpect(jsonPath("$.entityTypes[1].title").value("Владелец"));
    }

    @Test
    void operatorGetsForbiddenOnAudit() throws Exception {
        mvc.perform(get("/api/admin/audit").cookie(accessCookie("operator")))
                .andExpect(status().isForbidden());
    }

    private ResultActions login(String login, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}"));
    }

    private Cookie accessCookie(String login) throws Exception {
        return login(login, PASSWORD).andReturn().getResponse().getCookie(AuthCookies.ACCESS);
    }

    private void createUser(String login, Role role) {
        UserAccount user = new UserAccount(login, login, encoder.encode(PASSWORD), Set.of(role));
        user.setMustChangePassword(false);
        users.save(user);
    }
}

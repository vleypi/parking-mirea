package ru.mirea.parking.owner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.mirea.parking.domain.Role;
import ru.mirea.parking.domain.UserAccount;
import ru.mirea.parking.repository.UserAccountRepository;
import ru.mirea.parking.security.AuthCookies;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OwnerNoteTest {

    private static final String PASSWORD = "Password2026";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private UserAccountRepository users;
    @Autowired
    private PasswordEncoder encoder;

    private String ownerId;
    private String otherOwnerId;
    private Cookie author;
    private Cookie colleague;
    private Cookie viewer;
    private Cookie admin;

    @BeforeEach
    void setUp() throws Exception {
        OwnerTestData.clean(jdbc);
        ownerId = OwnerTestData.insert(jdbc, 1, "Иван Иванов", "+7-900-100-00-01", 1, "ACTIVE", "0", true, false).toString();
        otherOwnerId = OwnerTestData.insert(jdbc, 2, "Мария Петрова", "+7-900-100-00-02", 1, "ACTIVE", "0", true, false).toString();
        author = login("owner-note-author", Role.OPERATOR, "Анна Автор");
        colleague = login("owner-note-colleague", Role.OPERATOR, "Олег Коллега");
        viewer = login("owner-note-viewer", Role.VIEWER, "Вера Наблюдатель");
        admin = login("owner-note-admin", Role.ADMIN, "Админ");
    }

    @AfterEach
    void cleanNotes() {
        jdbc.update("delete from owner_note");
    }

    @Test
    void createdNoteKeepsAuthorAndTime() throws Exception {
        create(author, "Просит звонить только после 18:00", false)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author.displayName").value("Анна Автор"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.pinned").value(false))
                .andExpect(jsonPath("$.canEdit").value(true));
    }

    @Test
    void pinnedNotesComeFirstThenNewest() throws Exception {
        create(author, "первая", false);
        Thread.sleep(5);
        create(author, "закреплённая", true);
        Thread.sleep(5);
        create(colleague, "третья", false);

        list(viewer).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].body", contains("закреплённая", "третья", "первая")));
    }

    @Test
    void onlyAuthorOrAdminCanEditOrDelete() throws Exception {
        JsonNode note = body(create(author, "моя заметка", false));
        String noteUrl = "/api/owners/" + ownerId + "/notes/" + note.get("id").asText();

        list(colleague).andExpect(jsonPath("$[0].canEdit").value(false));
        update(colleague, noteUrl, "чужая правка", true, 0).andExpect(status().isForbidden());
        mvc.perform(delete(noteUrl).cookie(colleague)).andExpect(status().isForbidden());

        update(author, noteUrl, "моя правка", true, 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinned").value(true))
                .andExpect(jsonPath("$.version").value(1));
        update(admin, noteUrl, "правка админа", false, 1).andExpect(status().isOk());
        mvc.perform(delete(noteUrl).cookie(admin)).andExpect(status().isNoContent());
        list(author).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void staleVersionIsConflict() throws Exception {
        JsonNode note = body(create(author, "текст", false));
        String noteUrl = "/api/owners/" + ownerId + "/notes/" + note.get("id").asText();
        update(author, noteUrl, "новый текст", false, 0).andExpect(status().isOk());
        update(author, noteUrl, "устаревшая правка", false, 0).andExpect(status().isConflict());
    }

    @Test
    void viewerCanReadButNotWrite() throws Exception {
        create(author, "видна всем", false);
        list(viewer).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        create(viewer, "нельзя", false).andExpect(status().isForbidden());
    }

    @Test
    void notFoundCases() throws Exception {
        mvc.perform(get("/api/owners/" + UUID.randomUUID() + "/notes").cookie(author)).andExpect(status().isNotFound());
        JsonNode note = body(create(author, "заметка владельца 1", false));
        String wrongUrl = "/api/owners/" + otherOwnerId + "/notes/" + note.get("id").asText();
        update(author, wrongUrl, "x", false, 0).andExpect(status().isNotFound());
    }

    @Test
    void blankBodyIsRejected() throws Exception {
        create(author, "   ", false).andExpect(status().isBadRequest());
    }

    private ResultActions create(Cookie who, String text, boolean pinned) throws Exception {
        return mvc.perform(post("/api/owners/" + ownerId + "/notes").cookie(who)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("body", text, "pinned", pinned))));
    }

    private ResultActions update(Cookie who, String url, String text, boolean pinned, long version) throws Exception {
        return mvc.perform(put(url).cookie(who).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("body", text, "pinned", pinned, "version", version))));
    }

    private ResultActions list(Cookie who) throws Exception {
        return mvc.perform(get("/api/owners/" + ownerId + "/notes").cookie(who));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private Cookie login(String login, Role role, String name) throws Exception {
        if (users.findByLogin(login).isEmpty()) {
            UserAccount account = new UserAccount(login, name, encoder.encode(PASSWORD), Set.of(role));
            account.setMustChangePassword(false);
            users.save(account);
        }
        Cookie cookie = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("login", login, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie(AuthCookies.ACCESS);
        assertThat(cookie).isNotNull();
        return cookie;
    }
}

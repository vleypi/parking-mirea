package ru.mirea.parking.owner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "OPERATOR")
class OwnerBulkTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID ivan;
    private UUID maria;
    private UUID archivedAnna;

    @BeforeEach
    void seed() {
        OwnerTestData.clean(jdbc);
        jdbc.update("delete from audit_event");
        ivan = OwnerTestData.insert(jdbc, 1, "Иван Иванов", "+7-900-100-00-01", 1, "ACTIVE", "0", true, false);
        maria = OwnerTestData.insert(jdbc, 2, "Мария Петрова", "+7-900-100-00-02", 1, "ACTIVE", "0", true, false, "vip");
        archivedAnna = OwnerTestData.insert(jdbc, 3, "Анна Архивная", "+7-900-100-00-03", 1, "ACTIVE", "0", true, true);
    }

    @Test
    void archiveWithUnknownIdGivesSeparateSuccessesAndFailures() throws Exception {
        UUID unknown = UUID.randomUUID();
        bulk("ARCHIVE", ids(ivan, maria, archivedAnna, unknown), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.succeeded[*]", containsInAnyOrder(ivan.toString(), maria.toString())))
                .andExpect(jsonPath("$.failed.length()").value(2))
                .andExpect(jsonPath("$.failed[?(@.id == '%s')].reason".formatted(archivedAnna))
                        .value("Владелец Анна Архивная уже в архиве"))
                .andExpect(jsonPath("$.failed[?(@.id == '%s')].reason".formatted(unknown))
                        .value("Владелец " + unknown + " не найден"));

        assertThat(jdbc.queryForObject("select count(*) from users where archived = true", Long.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from audit_event where event_type = 'ARCHIVE'", Long.class)).isEqualTo(2);
    }

    @Test
    void changeStatusSkipsArchived() throws Exception {
        bulk("CHANGE_STATUS", ids(ivan, archivedAnna), "\"status\":\"BLOCKED\"")
                .andExpect(jsonPath("$.succeeded[*]", containsInAnyOrder(ivan.toString())))
                .andExpect(jsonPath("$.failed[0].reason").value("Архивного владельца изменить нельзя"));
        assertThat(jdbc.queryForObject("select status from users where id = 1", String.class)).isEqualTo("BLOCKED");
        assertThat(jdbc.queryForObject("select count(*) from audit_event where event_type = 'UPDATE'", Long.class)).isEqualTo(1);
    }

    @Test
    void addAndRemoveTag() throws Exception {
        bulk("ADD_TAG", ids(ivan, maria), "\"tag\":\" VIP \"").andExpect(jsonPath("$.succeeded.length()").value(2));
        assertThat(jdbc.queryForObject("select count(*) from owner_tag where tag = 'vip'", Long.class)).isEqualTo(2);

        bulk("REMOVE_TAG", ids(ivan, maria), "\"tag\":\"vip\"").andExpect(jsonPath("$.succeeded.length()").value(2));
        assertThat(jdbc.queryForObject("select count(*) from owner_tag", Long.class)).isZero();
    }

    @Test
    void restoreReturnsArchived() throws Exception {
        bulk("RESTORE", ids(archivedAnna, ivan), null)
                .andExpect(jsonPath("$.succeeded[*]", containsInAnyOrder(archivedAnna.toString())))
                .andExpect(jsonPath("$.failed[0].reason").value("Владелец Иван Иванов не в архиве"));
    }

    @Test
    void invalidRequestsAre400() throws Exception {
        bulk("CHANGE_STATUS", ids(ivan), null).andExpect(status().isBadRequest());
        bulk("ADD_TAG", ids(ivan), null).andExpect(status().isBadRequest());
        bulk("ARCHIVE", "", null).andExpect(status().isBadRequest());
        String tooMany = Collections.nCopies(201, "\"" + ivan + "\"").stream().collect(Collectors.joining(","));
        bulk("ARCHIVE", tooMany, null).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerIsForbidden() throws Exception {
        bulk("ARCHIVE", ids(ivan), null).andExpect(status().isForbidden());
    }

    private static String ids(UUID... ids) {
        return java.util.Arrays.stream(ids).map(id -> "\"" + id + "\"").collect(Collectors.joining(","));
    }

    private ResultActions bulk(String action, String ids, String extra) throws Exception {
        String json = "{\"ids\":[" + ids + "],\"action\":\"" + action + "\"" + (extra == null ? "" : "," + extra) + "}";
        return mvc.perform(post("/api/owners/bulk").contentType(MediaType.APPLICATION_JSON).content(json));
    }
}

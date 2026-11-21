package ru.mirea.parking.request;

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

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "OPERATOR")
class RequestArchiveTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    private String id;

    @BeforeEach
    void seed() {
        RequestTestData.seed(jdbc);
        id = jdbc.queryForObject("select uuid from requests where id = 1", UUID.class).toString();
    }

    @Test
    void archiveHidesFromDefaultListAndRestoreReturnsIt() throws Exception {
        mvc.perform(delete("/api/requests/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.archived").value(true));

        mvc.perform(get("/api/requests")).andExpect(jsonPath("$.content[*].id", not(hasItem(id))));
        mvc.perform(get("/api/requests?archived=ARCHIVED")).andExpect(jsonPath("$.content[*].id", hasItem(id)));
        mvc.perform(get("/api/requests/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.archived").value(true));

        mvc.perform(post("/api/requests/" + id + "/restore")).andExpect(status().isOk()).andExpect(jsonPath("$.archived").value(false));
        mvc.perform(get("/api/requests")).andExpect(jsonPath("$.content[*].id", hasItem(id)));
    }

    @Test
    void repeatedArchiveOrRestoreIsConflict() throws Exception {
        mvc.perform(post("/api/requests/" + id + "/restore")).andExpect(status().isConflict());
        mvc.perform(delete("/api/requests/" + id)).andExpect(status().isOk());
        mvc.perform(delete("/api/requests/" + id)).andExpect(status().isConflict());
    }

    @Test
    void archivedRequestCannotBeEdited() throws Exception {
        String vehicle = jdbc.queryForObject("select uuid from vehicles where id = 1", UUID.class).toString();
        String spot = jdbc.queryForObject("select uuid from spots where id = 1", UUID.class).toString();
        mvc.perform(delete("/api/requests/" + id)).andExpect(status().isOk());

        mvc.perform(put("/api/requests/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"vehicleId":"%s","spotId":"%s","startTime":"2030-01-10T09:00:00","endTime":"2030-01-10T11:00:00",
                         "status":"NEW","paid":false,"version":1}""".formatted(vehicle, spot)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Архивную заявку изменить нельзя"));
    }

    @Test
    void unknownRequestIs404() throws Exception {
        mvc.perform(delete("/api/requests/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerCannotArchive() throws Exception {
        mvc.perform(delete("/api/requests/" + id)).andExpect(status().isForbidden());
        mvc.perform(post("/api/requests/" + id + "/restore")).andExpect(status().isForbidden());
    }

    @Test
    void operatorCannotDeletePermanently() throws Exception {
        mvc.perform(delete("/api/requests/" + id + "/permanent")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void permanentDeleteIsForbiddenWhileFlagIsOff() throws Exception {
        mvc.perform(delete("/api/requests/" + id + "/permanent"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Окончательное удаление отключено настройкой PERMANENT_DELETE_ENABLED"));
        mvc.perform(get("/api/requests/" + id)).andExpect(status().isOk());
    }
}

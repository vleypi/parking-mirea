package ru.mirea.parking.reference;

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
import ru.mirea.parking.TestDataCleaner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Справочник типов мест и общий GET /api/references. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReferenceTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        TestDataCleaner.clean(jdbc);
        TestDataCleaner.seedReferences(jdbc);
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void allReferencesInOneRequest() throws Exception {
        mvc.perform(get("/api/references"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spotTypes[*].code", contains("STANDARD", "DISABLED", "ELECTRIC")))
                .andExpect(jsonPath("$.requestStatuses[*].code", contains("NEW", "CONFIRMED", "COMPLETED", "CANCELLED")))
                .andExpect(jsonPath("$.clientCategories[*].code", hasItem("RESIDENT")))
                .andExpect(jsonPath("$.ownerStatuses[*].code", hasItem("BLOCKED")))
                .andExpect(jsonPath("$.vehicleStatuses[*].code", hasItem("DEREGISTERED")))
                .andExpect(jsonPath("$.spotStatuses[*].code", hasItem("MAINTENANCE")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminAddsRenamesAndDisablesTypeWithoutDeleting() throws Exception {
        mvc.perform(post("/api/references/spot-types").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"family\",\"title\":\"Семейное\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.code").value("FAMILY"))
                .andExpect(jsonPath("$.active").value(true));

        mvc.perform(put("/api/references/spot-types/4").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"FAMILY\",\"title\":\"Для семей с детьми\",\"sortOrder\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Для семей с детьми"));

        mvc.perform(patch("/api/references/spot-types/4/active").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        assertThat(jdbc.queryForObject("select count(*) from spot_types", Integer.class)).isEqualTo(4);
        mvc.perform(get("/api/references")).andExpect(jsonPath("$.spotTypes[0].code").value("FAMILY"));
        assertThat(jdbc.queryForObject("select count(*) from audit_event where entity_type = 'REFERENCE'", Integer.class))
                .isEqualTo(3);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void duplicateCodeOrTitleIsConflict() throws Exception {
        mvc.perform(post("/api/references/spot-types").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"standard\",\"title\":\"Другое\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/references/spot-types").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"OTHER\",\"title\":\"Стандартное\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/references/spot-types").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"1bad\",\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("code")));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void operatorCannotChangeReferences() throws Exception {
        mvc.perform(post("/api/references/spot-types").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"VIP\",\"title\":\"VIP\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/references/spot-types/1/active").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void unknownTypeIs404() throws Exception {
        mvc.perform(patch("/api/references/spot-types/99/active").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isNotFound());
    }
}

package ru.mirea.parking.owner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.permanent-delete-enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class OwnerPermanentDeleteTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        OwnerTestData.clean(jdbc);
        jdbc.update("delete from audit_event");
    }

    @Test
    void ownerWithoutLinksIsDeletedWithTags() throws Exception {
        UUID owner = OwnerTestData.insert(jdbc, 1, "Иван Иванов", "+7-900-100-00-01", 1, "ACTIVE", "0", true, true, "vip");

        mvc.perform(delete("/api/owners/" + owner + "/permanent")).andExpect(status().isNoContent());

        mvc.perform(get("/api/owners/" + owner)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from owner_tag", Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from audit_event where event_type = 'DELETE' and entity_id = ?",
                Long.class, owner)).isEqualTo(1);
    }

    @Test
    void ownerWithVehiclesIsConflict() throws Exception {
        UUID owner = OwnerTestData.insert(jdbc, 1, "Иван Иванов", "+7-900-100-00-01", 1, "ACTIVE", "0", true, false);
        jdbc.update("insert into vehicles (id, uuid, user_id, license_plate, brand, model) values (1, ?, 1, 'А123ВС777', 'Toyota', 'Camry')",
                UUID.randomUUID());

        mvc.perform(delete("/api/owners/" + owner + "/permanent"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Владельца Иван Иванов нельзя удалить окончательно: у владельца есть автомобили или заявки"));
        mvc.perform(get("/api/owners/" + owner)).andExpect(status().isOk());
    }
}

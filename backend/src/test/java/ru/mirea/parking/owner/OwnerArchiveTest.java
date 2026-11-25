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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
class OwnerArchiveTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID ivan;

    @BeforeEach
    void seed() {
        OwnerTestData.clean(jdbc);
        jdbc.update("delete from audit_event");
        ivan = OwnerTestData.insert(jdbc, 1, "Иван Иванов", "+7-900-100-00-01", 1, "ACTIVE", "0", true, false);
    }

    @Test
    void archiveAndRestoreAreRecordedInAudit() throws Exception {
        mvc.perform(delete("/api/owners/" + ivan)).andExpect(status().isOk()).andExpect(jsonPath("$.archived").value(true));
        mvc.perform(get("/api/owners/" + ivan)).andExpect(status().isOk()).andExpect(jsonPath("$.archived").value(true));
        mvc.perform(post("/api/owners/" + ivan + "/restore")).andExpect(status().isOk()).andExpect(jsonPath("$.archived").value(false));

        assertThat(jdbc.queryForList("select event_type from audit_event where entity_type = 'OWNER' order by occurred_at", String.class))
                .containsExactly("ARCHIVE", "RESTORE");
    }

    @Test
    void repeatedArchiveOrRestoreIsConflict() throws Exception {
        mvc.perform(post("/api/owners/" + ivan + "/restore")).andExpect(status().isConflict());
        mvc.perform(delete("/api/owners/" + ivan)).andExpect(status().isOk());
        mvc.perform(delete("/api/owners/" + ivan)).andExpect(status().isConflict());
    }

    @Test
    void archivedOwnerCannotBeEdited() throws Exception {
        mvc.perform(delete("/api/owners/" + ivan)).andExpect(status().isOk());
        mvc.perform(put("/api/owners/" + ivan).contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"Иван","phone":"+7-900-100-00-01","categoryId":1,"status":"ACTIVE",
                         "discountPercent":0,"notificationsEnabled":true,"version":1}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Архивного владельца изменить нельзя"));
    }

    @Test
    void archivedOwnerCannotGetNewRequest() throws Exception {
        jdbc.update("insert into request_statuses (id, code, title) values (1, 'NEW', 'Новая'), (2, 'CONFIRMED', 'Подтверждена'), "
                + "(3, 'COMPLETED', 'Завершена'), (4, 'CANCELLED', 'Отменена')");
        jdbc.update("insert into spot_types (id, code, title) values (1, 'STANDARD', 'Стандартное')");
        UUID car = UUID.randomUUID();
        UUID spot = UUID.randomUUID();
        jdbc.update("insert into vehicles (id, uuid, user_id, license_plate, brand, model) values (1, ?, 1, 'А123ВС777', 'Toyota', 'Camry')", car);
        jdbc.update("insert into spots (id, uuid, spot_number, spot_type_id, hourly_rate) values (1, ?, 1, 1, 100.00)", spot);
        mvc.perform(delete("/api/owners/" + ivan)).andExpect(status().isOk());

        mvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON).content("""
                        {"ownerId":"%s","vehicleId":"%s","spotId":"%s",
                         "startTime":"2030-01-10T09:00:00","endTime":"2030-01-10T11:00:00"}""".formatted(ivan, car, spot)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Владелец Иван Иванов в архиве: новую заявку оформить нельзя"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerCannotArchive() throws Exception {
        mvc.perform(delete("/api/owners/" + ivan)).andExpect(status().isForbidden());
        mvc.perform(post("/api/owners/" + ivan + "/restore")).andExpect(status().isForbidden());
    }

    @Test
    void operatorCannotDeletePermanently() throws Exception {
        mvc.perform(delete("/api/owners/" + ivan + "/permanent")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void permanentDeleteIsForbiddenWhileFlagIsOff() throws Exception {
        mvc.perform(delete("/api/owners/" + ivan + "/permanent"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Окончательное удаление отключено настройкой PERMANENT_DELETE_ENABLED"));
        mvc.perform(get("/api/owners/" + ivan)).andExpect(status().isOk());
    }

    @Test
    void unknownOwnerIs404() throws Exception {
        mvc.perform(delete("/api/owners/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }
}

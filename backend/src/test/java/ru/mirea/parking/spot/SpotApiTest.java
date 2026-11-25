package ru.mirea.parking.spot;

import org.junit.jupiter.api.AfterEach;
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
import ru.mirea.parking.TestDataCleaner;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "OPERATOR")
class SpotApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID first;

    @BeforeEach
    void seed() {
        SpotTestData.seed(jdbc);
        first = SpotTestData.spot(jdbc, 1, 1, 1, "100.00", "A", "У въезда", "AVAILABLE", true, false, "у входа");
    }

    @AfterEach
    void clean() {
        TestDataCleaner.clean(jdbc);
    }

    @Test
    void getReturnsTypeTitleAndAvailability() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        SpotTestData.request(jdbc, 1, 1, 2, now.minusHours(1), now.plusHours(1));
        mvc.perform(get("/api/spots/" + first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spotNumber").value(1))
                .andExpect(jsonPath("$.spotType.title").value("Стандартное"))
                .andExpect(jsonPath("$.hourlyRate").value(100.00))
                .andExpect(jsonPath("$.covered").value(true))
                .andExpect(jsonPath("$.status.code").value("AVAILABLE"))
                .andExpect(jsonPath("$.availability.code").value("BUSY_NOW"))
                .andExpect(jsonPath("$.tags[0]").value("у входа"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void createWithDefaultsAndAudit() throws Exception {
        create("""
                {"spotNumber":7,"spotTypeId":3,"hourlyRate":150.5,"zone":" B ","tags":[" Зарядка ","зарядка"]}""")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/spots/")))
                .andExpect(jsonPath("$.spotType.code").value("ELECTRIC"))
                .andExpect(jsonPath("$.hourlyRate").value(150.50))
                .andExpect(jsonPath("$.zone").value("B"))
                .andExpect(jsonPath("$.status.code").value("AVAILABLE"))
                .andExpect(jsonPath("$.covered").value(false))
                .andExpect(jsonPath("$.availability.code").value("FREE_NOW"))
                .andExpect(jsonPath("$.tags.length()").value(1));
        Integer audit = jdbc.queryForObject(
                "select count(*) from audit_event where event_type = 'CREATE' and entity_type = 'SPOT'", Integer.class);
        assertThat(audit).isEqualTo(1);
    }

    @Test
    void invalidFieldsAre400WithFieldNames() throws Exception {
        create("""
                {"spotNumber":0,"hourlyRate":-5,"zone":"очень длинное название зоны"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'hourlyRate')]").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'spotNumber')]").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'spotTypeId')]").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'zone')]").isNotEmpty());
        create("""
                {"spotNumber":7,"spotTypeId":1,"hourlyRate":100000}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("hourlyRate"));
    }

    @Test
    void unknownOrDisabledTypeIs400() throws Exception {
        create("""
                {"spotNumber":7,"spotTypeId":99,"hourlyRate":100}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("spotTypeId"));
        jdbc.update("update spot_types set active = false where id = 2");
        create("""
                {"spotNumber":7,"spotTypeId":2,"hourlyRate":100}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("отключён")));
    }

    @Test
    void busyNumberIsConflict() throws Exception {
        create("""
                {"spotNumber":1,"spotTypeId":1,"hourlyRate":100}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Место с номером 1 уже есть"));
    }

    @Test
    void updateChecksVersion() throws Exception {
        update(0, "MAINTENANCE").andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value("MAINTENANCE"))
                .andExpect(jsonPath("$.availability.code").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.version").value(1));
        update(0, "AVAILABLE").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Запись изменил другой пользователь, обновите карточку"));
    }

    @Test
    void unknownIdIs404() throws Exception {
        mvc.perform(get("/api/spots/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerCanReadButNotWrite() throws Exception {
        mvc.perform(get("/api/spots")).andExpect(status().isOk());
        create("""
                {"spotNumber":7,"spotTypeId":1,"hourlyRate":100}""")
                .andExpect(status().isForbidden());
    }

    @Test
    void requestCannotBeCreatedForSpotInMaintenance() throws Exception {
        jdbc.update("update spots set status = 'MAINTENANCE' where id = 1");
        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        mvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON).content("""
                        {"ownerId":"%s","vehicleId":"%s","spotId":"%s","startTime":"%s","endTime":"%s"}""".formatted(
                        SpotTestData.ownerUuid(jdbc), SpotTestData.vehicleUuid(jdbc), first, start, start.plusHours(2))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("На обслуживании")));
    }

    @Test
    void archiveBlockedByUnfinishedActiveRequest() throws Exception {
        SpotTestData.request(jdbc, 1, 1, 1, LocalDateTime.now().plusDays(2));
        mvc.perform(delete("/api/spots/" + first))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("ещё не закончилась")));

        jdbc.update("update requests set status_id = 4 where id = 1");
        mvc.perform(delete("/api/spots/" + first)).andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
        mvc.perform(delete("/api/spots/" + first)).andExpect(status().isConflict());
        mvc.perform(post("/api/spots/" + first + "/restore")).andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false));
        Integer audit = jdbc.queryForObject(
                "select count(*) from audit_event where entity_type = 'SPOT' and event_type in ('ARCHIVE', 'RESTORE')",
                Integer.class);
        assertThat(audit).isEqualTo(2);
    }

    @Test
    void permanentDeleteIsAdminOnly() throws Exception {
        mvc.perform(delete("/api/spots/" + first + "/permanent")).andExpect(status().isForbidden());
    }

    @Test
    void requestsForCard() throws Exception {
        SpotTestData.request(jdbc, 1, 1, 3, LocalDateTime.now().minusDays(3));
        SpotTestData.request(jdbc, 2, 1, 1, LocalDateTime.now().plusDays(1));
        mvc.perform(get("/api/spots/" + first + "/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.requests[0].code").value("REQ-000002"))
                .andExpect(jsonPath("$.requests[0].licensePlate").value("А123ВС777"));
    }

    private ResultActions create(String body) throws Exception {
        return mvc.perform(post("/api/spots").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions update(long version, String status) throws Exception {
        return mvc.perform(put("/api/spots/" + first).contentType(MediaType.APPLICATION_JSON).content("""
                {"spotNumber":1,"spotTypeId":1,"hourlyRate":120,"zone":"A","status":"%s","covered":true,
                 "tags":["у входа"],"version":%d}""".formatted(status, version)));
    }
}

package ru.mirea.parking.vehicle;

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
import static org.hamcrest.Matchers.hasItem;
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
class VehicleApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID ivan;
    private UUID camry;

    @BeforeEach
    void seed() {
        VehicleTestData.seed(jdbc);
        ivan = VehicleTestData.ownerUuid(jdbc, 1);
        camry = VehicleTestData.vehicle(jdbc, 1, 1, "А123ВС777", "Toyota", "Camry", 2018, false, "ACTIVE", false, "vip");
    }

    @AfterEach
    void clean() {
        TestDataCleaner.clean(jdbc);
    }

    @Test
    void getReturnsDtoWithOwnerRegionAndActivity() throws Exception {
        VehicleTestData.request(jdbc, 1, 1, 1, LocalDateTime.now().minusDays(2));
        mvc.perform(get("/api/vehicles/" + camry))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensePlate").value("А123ВС777"))
                .andExpect(jsonPath("$.region").value("777"))
                .andExpect(jsonPath("$.owner.name").value("Иван Иванов"))
                .andExpect(jsonPath("$.status.code").value("ACTIVE"))
                .andExpect(jsonPath("$.recentRequests").value(1))
                .andExpect(jsonPath("$.activity.code").value("MEDIUM"))
                .andExpect(jsonPath("$.tags[0]").value("vip"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void createNormalizesPlateAndWritesAudit() throws Exception {
        create("""
                {"ownerId":"%s","licensePlate":" b456km177 ","brand":" Lada ","model":"Vesta","releaseYear":2020,
                 "tags":[" Ночь ","ночь"]}""".formatted(ivan))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/vehicles/")))
                .andExpect(jsonPath("$.licensePlate").value("В456КМ177"))
                .andExpect(jsonPath("$.brand").value("Lada"))
                .andExpect(jsonPath("$.status.code").value("ACTIVE"))
                .andExpect(jsonPath("$.electric").value(false))
                .andExpect(jsonPath("$.activity.code").value("LOW"))
                .andExpect(jsonPath("$.tags.length()").value(1));
        Integer audit = jdbc.queryForObject(
                "select count(*) from audit_event where event_type = 'CREATE' and entity_type = 'VEHICLE'", Integer.class);
        assertThat(audit).isEqualTo(1);
    }

    @Test
    void invalidFieldsAre400WithFieldNames() throws Exception {
        create("""
                {"ownerId":"%s","licensePlate":"123","brand":"","model":"X","releaseYear":1950}""".formatted(ivan))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'licensePlate')].message", hasItem(containsString("А123ВС777"))))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'brand')]").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'releaseYear')]").isNotEmpty());
    }

    @Test
    void futureYearIs400() throws Exception {
        create("""
                {"ownerId":"%s","licensePlate":"В456КМ177","brand":"Lada","model":"Vesta","releaseYear":%d}"""
                .formatted(ivan, LocalDateTime.now().getYear() + 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("releaseYear"));
    }

    @Test
    void duplicatePlateIsConflictEvenInLatin() throws Exception {
        create("""
                {"ownerId":"%s","licensePlate":"A123BC777","brand":"Lada","model":"Vesta"}""".formatted(ivan))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Автомобиль с гос. номером А123ВС777 уже есть"));
    }

    @Test
    void archivedOwnerIsConflictAndUnknownOwnerIs404() throws Exception {
        create("""
                {"ownerId":"%s","licensePlate":"В456КМ177","brand":"Lada","model":"Vesta"}"""
                .formatted(VehicleTestData.ownerUuid(jdbc, 3)))
                .andExpect(status().isConflict());
        create("""
                {"ownerId":"%s","licensePlate":"В456КМ177","brand":"Lada","model":"Vesta"}""".formatted(UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateChecksVersion() throws Exception {
        update(0).andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value("BLOCKED"))
                .andExpect(jsonPath("$.version").value(1));
        update(0).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Запись изменил другой пользователь, обновите карточку"));
    }

    @Test
    void unknownIdIs404() throws Exception {
        mvc.perform(get("/api/vehicles/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerCanReadButNotWrite() throws Exception {
        mvc.perform(get("/api/vehicles")).andExpect(status().isOk());
        create("""
                {"ownerId":"%s","licensePlate":"В456КМ177","brand":"Lada","model":"Vesta"}""".formatted(ivan))
                .andExpect(status().isForbidden());
    }

    @Test
    void archiveRestoreAndRequestRules() throws Exception {
        mvc.perform(delete("/api/vehicles/" + camry)).andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));
        mvc.perform(delete("/api/vehicles/" + camry)).andExpect(status().isConflict());
        update(1).andExpect(status().isConflict());
        mvc.perform(get("/api/vehicles")).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/vehicles").param("archived", "ARCHIVED")).andExpect(jsonPath("$.totalElements").value(1));

        LocalDateTime start = LocalDateTime.now().plusDays(1).withNano(0);
        String request = """
                {"ownerId":"%s","vehicleId":"%s","spotId":"%s","startTime":"%s","endTime":"%s"}""".formatted(ivan, camry,
                jdbc.queryForObject("select uuid from spots where id = 1", UUID.class), start, start.plusHours(2));
        mvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("в архиве")));

        mvc.perform(post("/api/vehicles/" + camry + "/restore")).andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(false));
        jdbc.update("update vehicles set status = 'BLOCKED' where id = 1");
        mvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Въезд запрещён")));
    }

    @Test
    void permanentDeleteIsAdminOnly() throws Exception {
        mvc.perform(delete("/api/vehicles/" + camry + "/permanent")).andExpect(status().isForbidden());
    }

    @Test
    void lastRequestsForCard() throws Exception {
        VehicleTestData.request(jdbc, 1, 1, 3, LocalDateTime.now().minusDays(40));
        VehicleTestData.request(jdbc, 2, 1, 1, LocalDateTime.now().plusDays(1));
        mvc.perform(get("/api/vehicles/" + camry + "/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.requests[0].code").value("REQ-000002"));
    }

    private ResultActions create(String body) throws Exception {
        return mvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions update(long version) throws Exception {
        return mvc.perform(put("/api/vehicles/" + camry).contentType(MediaType.APPLICATION_JSON).content("""
                {"ownerId":"%s","licensePlate":"А123ВС777","brand":"Toyota","model":"Camry","releaseYear":2018,
                 "electric":false,"status":"BLOCKED","tags":["vip"],"version":%d}""".formatted(ivan, version)));
    }
}

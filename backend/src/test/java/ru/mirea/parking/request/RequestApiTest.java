package ru.mirea.parking.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
class RequestApiTest {

    private static final UUID IVAN = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MARIA = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID IVAN_CAR = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID IVAN_TESLA = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID MARIA_CAR = UUID.fromString("00000000-0000-0000-0000-000000000013");
    private static final UUID SPOT_1 = UUID.fromString("00000000-0000-0000-0000-000000000021");
    private static final UUID SPOT_2 = UUID.fromString("00000000-0000-0000-0000-000000000022");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper json;

    @BeforeEach
    void seed() {
        for (String table : List.of("request_tag", "requests", "vehicles", "spots", "users", "request_statuses")) {
            jdbc.update("delete from " + table);
        }
        jdbc.update("insert into request_statuses (id, code, title) values "
                + "(1, 'NEW', 'Новая'), (2, 'CONFIRMED', 'Подтверждена'), (3, 'COMPLETED', 'Завершена'), (4, 'CANCELLED', 'Отменена')");
        jdbc.update("insert into users (id, uuid, name, phone, created_at) values "
                + "(1, ?, 'Иван Иванов', '+7-900-100-00-01', now()), (2, ?, 'Мария Петрова', '+7-900-100-00-02', now())",
                IVAN, MARIA);
        jdbc.update("insert into vehicles (id, uuid, user_id, license_plate, brand, model) values "
                + "(1, ?, 1, 'А123ВС777', 'Toyota', 'Camry'), (2, ?, 1, 'М777АА777', 'Tesla', 'Model 3'), "
                + "(3, ?, 2, 'В456КМ177', 'Kia', 'Rio')", IVAN_CAR, IVAN_TESLA, MARIA_CAR);
        jdbc.update("insert into spots (id, uuid, spot_number, hourly_rate) values (1, ?, 1, 100.00), (2, ?, 2, 150.00)",
                SPOT_1, SPOT_2);
    }

    @Test
    void createsRequestAndCalculatesCost() throws Exception {
        create(body(IVAN, IVAN_CAR, SPOT_2, "2026-12-01T08:00:00", "2026-12-01T10:30:00"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status.code").value("NEW"))
                .andExpect(jsonPath("$.priority").exists())
                .andExpect(jsonPath("$.cost").value(375.00))
                .andExpect(jsonPath("$.owner.name").value("Иван Иванов"))
                .andExpect(jsonPath("$.vehicle.licensePlate").value("А123ВС777"))
                .andExpect(jsonPath("$.tags[0]").value("vip"))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void getsById() throws Exception {
        String id = createAndGetId(body(IVAN, IVAN_CAR, SPOT_1, "2026-12-01T08:00:00", "2026-12-01T09:00:00"));
        mvc.perform(get("/api/requests/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.spot.number").value(1));
    }

    @Test
    void unknownIdIs404AndMalformedIdIs400() throws Exception {
        mvc.perform(get("/api/requests/" + UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/requests/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void invalidBodyIs400() throws Exception {
        create(Map.of("ownerId", IVAN.toString())).andExpect(status().isBadRequest());
        create(body(IVAN, IVAN_CAR, SPOT_1, "2026-12-01T10:00:00", "2026-12-01T09:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Дата и время окончания должны быть позже даты и времени начала"));
    }

    @Test
    void vehicleMustBelongToOwner() throws Exception {
        create(body(IVAN, MARIA_CAR, SPOT_1, "2026-12-01T08:00:00", "2026-12-01T09:00:00"))
                .andExpect(status().isConflict());
    }

    @Test
    void spotAndVehicleCannotBeDoubleBooked() throws Exception {
        create(body(IVAN, IVAN_CAR, SPOT_1, "2026-12-01T08:00:00", "2026-12-01T12:00:00")).andExpect(status().isCreated());

        create(body(MARIA, MARIA_CAR, SPOT_1, "2026-12-01T11:00:00", "2026-12-01T13:00:00"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Место 1 уже занято на указанный период"));
        create(body(IVAN, IVAN_CAR, SPOT_2, "2026-12-01T09:00:00", "2026-12-01T10:00:00"))
                .andExpect(status().isConflict());
        create(body(MARIA, MARIA_CAR, SPOT_1, "2026-12-01T12:00:00", "2026-12-01T13:00:00"))
                .andExpect(status().isCreated());
    }

    @Test
    void updateChecksVersionAndStatusTransitions() throws Exception {
        String id = createAndGetId(body(IVAN, IVAN_CAR, SPOT_1, "2026-12-01T08:00:00", "2026-12-01T10:00:00"));

        update(id, updateBody(IVAN_TESLA, SPOT_2, "CONFIRMED", 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value("CONFIRMED"))
                .andExpect(jsonPath("$.vehicle.licensePlate").value("М777АА777"))
                .andExpect(jsonPath("$.cost").value(300.00))
                .andExpect(jsonPath("$.version").value(1));

        update(id, updateBody(IVAN_TESLA, SPOT_2, "CONFIRMED", 0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Запись изменил другой пользователь, обновите карточку"));

        update(id, updateBody(IVAN_TESLA, SPOT_2, "NEW", 1)).andExpect(status().isConflict());
        update(id, updateBody(IVAN_TESLA, SPOT_2, "COMPLETED", 1)).andExpect(status().isOk());
        update(id, updateBody(IVAN_TESLA, SPOT_2, "COMPLETED", 2)).andExpect(status().isConflict());
    }

    @Test
    void updateWithAnotherOwnersCarIsRejected() throws Exception {
        String id = createAndGetId(body(IVAN, IVAN_CAR, SPOT_1, "2026-12-01T08:00:00", "2026-12-01T10:00:00"));
        update(id, updateBody(MARIA_CAR, SPOT_1, "NEW", 0)).andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerCanReadButNotWrite() throws Exception {
        create(body(IVAN, IVAN_CAR, SPOT_1, "2026-12-01T08:00:00", "2026-12-01T09:00:00"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/requests/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    private Map<String, Object> body(UUID owner, UUID vehicle, UUID spot, String start, String end) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ownerId", owner);
        body.put("vehicleId", vehicle);
        body.put("spotId", spot);
        body.put("startTime", start);
        body.put("endTime", end);
        body.put("description", "Тестовая заявка");
        body.put("tags", List.of(" VIP "));
        return body;
    }

    private Map<String, Object> updateBody(UUID vehicle, UUID spot, String status, long version) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vehicleId", vehicle);
        body.put("spotId", spot);
        body.put("startTime", "2026-12-01T08:00:00");
        body.put("endTime", "2026-12-01T10:00:00");
        body.put("status", status);
        body.put("paid", true);
        body.put("version", version);
        return body;
    }

    private ResultActions create(Object body) throws Exception {
        return mvc.perform(post("/api/requests").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private ResultActions update(String id, Object body) throws Exception {
        return mvc.perform(put("/api/requests/" + id).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private String createAndGetId(Object body) throws Exception {
        String response = create(body).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(response);
        return node.get("id").asText();
    }
}

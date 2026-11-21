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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Карточка владельца: его машины и последние заявки, чужие записи не попадают. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "VIEWER")
class OwnerRelatedTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 3, 1, 9, 0);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID ivan;
    private UUID maria;
    private UUID archivedOwner;

    @BeforeEach
    void seed() {
        OwnerTestData.clean(jdbc);
        jdbc.update("insert into request_statuses (id, code, title) values (1, 'NEW', 'Новая'), (3, 'COMPLETED', 'Завершена')");
        jdbc.update("insert into spot_types (id, code, title) values (1, 'STANDARD', 'Стандартное')");
        jdbc.update("insert into spots (id, uuid, spot_number, spot_type_id, hourly_rate) values (1, ?, 7, 1, 100.00)", UUID.randomUUID());
        ivan = OwnerTestData.insert(jdbc, 1, "Иван Иванов", "+7-900-100-00-01", 1, "ACTIVE", "0", true, false);
        maria = OwnerTestData.insert(jdbc, 2, "Мария Петрова", "+7-900-100-00-02", 1, "ACTIVE", "0", true, false);
        archivedOwner = OwnerTestData.insert(jdbc, 3, "Анна Архивная", "+7-900-100-00-03", 1, "ACTIVE", "0", true, true);
        insertVehicle(1, 1, "В222ВВ77");
        insertVehicle(2, 1, "А111АА77");
        insertVehicle(3, 2, "С333СС77");
        insertVehicle(4, 3, "Е444ЕЕ77");
        insertRequest(1, 1, 1, 3, START, false);
        insertRequest(2, 1, 2, 1, START.plusDays(2), true);
        insertRequest(3, 2, 3, 1, START.plusDays(1), false);
        insertRequest(4, 3, 4, 3, START, true);
    }

    @Test
    void showsOwnVehiclesAndRequestsNewestFirst() throws Exception {
        mvc.perform(get("/api/owners/" + ivan + "/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicles[*].licensePlate", contains("А111АА77", "В222ВВ77")))
                .andExpect(jsonPath("$.requests[*].code", contains("REQ-000002", "REQ-000001")))
                .andExpect(jsonPath("$.requests[0].licensePlate").value("А111АА77"))
                .andExpect(jsonPath("$.requests[0].spotNumber").value(7))
                .andExpect(jsonPath("$.requests[0].statusTitle").value("Новая"))
                .andExpect(jsonPath("$.requests[0].archived").value(true))
                .andExpect(jsonPath("$.requestsTotal").value(2));
        mvc.perform(get("/api/owners/" + maria + "/related"))
                .andExpect(jsonPath("$.vehicles", hasSize(1)))
                .andExpect(jsonPath("$.requests[*].code", contains("REQ-000003")));
    }

    @Test
    void archivedOwnerStillShowsHistory() throws Exception {
        mvc.perform(get("/api/owners/" + archivedOwner + "/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicles[*].licensePlate", contains("Е444ЕЕ77")))
                .andExpect(jsonPath("$.requests[0].statusCode").value("COMPLETED"));
    }

    @Test
    void onlyLastTwentyRequestsButTotalIsFull() throws Exception {
        for (int i = 0; i < 22; i++) {
            insertRequest(100 + i, 2, 3, 1, START.plusDays(10 + i), false);
        }
        mvc.perform(get("/api/owners/" + maria + "/related"))
                .andExpect(jsonPath("$.requests", hasSize(20)))
                .andExpect(jsonPath("$.requests[0].code").value("REQ-000121"))
                .andExpect(jsonPath("$.requestsTotal").value(23));
    }

    @Test
    void unknownOwnerIs404() throws Exception {
        mvc.perform(get("/api/owners/" + UUID.randomUUID() + "/related")).andExpect(status().isNotFound());
    }

    private void insertVehicle(long id, long owner, String plate) {
        jdbc.update("insert into vehicles (id, uuid, user_id, license_plate, brand, model) values (?, ?, ?, ?, 'Lada', 'Vesta')",
                id, UUID.randomUUID(), owner, plate);
    }

    private void insertRequest(int id, long owner, long vehicle, int status, LocalDateTime start, boolean archived) {
        jdbc.update("insert into requests (id, uuid, code, user_id, vehicle_id, spot_id, status_id, start_time, end_time, "
                        + "cost, paid, archived, created_at, updated_at, version) values (?, ?, ?, ?, ?, 1, ?, ?, ?, 100, false, ?, now(), now(), 0)",
                id, UUID.randomUUID(), "REQ-%06d".formatted(id), owner, vehicle, status, start, start.plusHours(1), archived);
    }
}

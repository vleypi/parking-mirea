package ru.mirea.parking.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "VIEWER")
class RequestSearchTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2030, 1, 10, 9, 0);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        for (String table : List.of("request_tag", "requests", "vehicles", "spots", "spot_types", "users", "request_statuses")) {
            jdbc.update("delete from " + table);
        }
        jdbc.update("insert into spot_types (id, code, title) values (1, 'STANDARD', 'Стандартное'), (3, 'ELECTRIC', 'Для электромобилей')");
        jdbc.update("insert into request_statuses (id, code, title) values "
                + "(1, 'NEW', 'Новая'), (2, 'CONFIRMED', 'Подтверждена'), (3, 'COMPLETED', 'Завершена'), (4, 'CANCELLED', 'Отменена')");
        jdbc.update("insert into users (id, uuid, name, phone, created_at) values "
                + "(1, ?, 'Иван Иванов', '+7-900-100-00-01', now()), (2, ?, 'Мария O''Нил_100%', '+7-900-100-00-02', now())",
                UUID.randomUUID(), UUID.randomUUID());
        jdbc.update("insert into vehicles (id, uuid, user_id, license_plate, brand, model) values "
                + "(1, ?, 1, 'А123ВС777', 'Toyota', 'Camry'), (2, ?, 2, 'В456КМ177', 'Tesla', 'Model 3')",
                UUID.randomUUID(), UUID.randomUUID());
        jdbc.update("insert into spots (id, uuid, spot_number, spot_type_id, hourly_rate) values "
                + "(1, ?, 1, 1, 100.00), (2, ?, 5, 3, 150.00)", UUID.randomUUID(), UUID.randomUUID());

        insert(1, "REQ-000001", 1, 1, 1, 1, BASE, "100.00", false, false);
        insert(2, "REQ-000002", 1, 1, 1, 2, BASE.plusDays(1), "200.00", true, false);
        insert(3, "REQ-000003", 2, 2, 2, 1, BASE.plusDays(2), "300.00", false, false);
        insert(4, "REQ-000004", 2, 2, 2, 3, BASE.plusDays(3), "450.00", true, false);
        insert(5, "REQ-000005", 1, 1, 2, 4, BASE.plusDays(4), "50.00", false, true);
        jdbc.update("insert into request_tag (request_id, tag) values (1, 'vip'), (3, 'vip'), (3, 'ночь'), (4, 'ночь')");
    }

    @Test
    void returnsPageWithMetadataAndHidesArchivedByDefault() throws Exception {
        search("").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalPages").value(1));
        search("archived=archived").andExpect(jsonPath("$.content[*].code", contains("REQ-000005")));
        search("archived=ALL").andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void textSearchByCodePlateAndOwnerName() throws Exception {
        search("q=req-000003").andExpect(jsonPath("$.content[*].code", contains("REQ-000003")));
        search("q=а123").andExpect(jsonPath("$.content[*].code", containsInAnyOrder("REQ-000001", "REQ-000002")));
        search("q=иванов").andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void specialCharactersInQueryAreSafe() throws Exception {
        search("q=" + enc("O'Нил")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        search("q=" + enc("_100%")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        search("q=" + enc("%")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        search("q=" + enc("'; drop table requests; --")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        search("archived=ALL").andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void filtersCombine() throws Exception {
        search("status=new").andExpect(jsonPath("$.content[*].code", containsInAnyOrder("REQ-000001", "REQ-000003")));
        search("spotType=ELECTRIC").andExpect(jsonPath("$.content[*].code", containsInAnyOrder("REQ-000003", "REQ-000004")));
        search("paid=true").andExpect(jsonPath("$.content[*].code", containsInAnyOrder("REQ-000002", "REQ-000004")));
        search("tag=VIP").andExpect(jsonPath("$.content[*].code", containsInAnyOrder("REQ-000001", "REQ-000003")));
        search("tag=vip&spotType=electric&status=NEW").andExpect(jsonPath("$.content[*].code", contains("REQ-000003")));
        search("priority=low").andExpect(jsonPath("$.totalElements").value(4));
        search("priority=HIGH").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void costComparisonAndPeriod() throws Exception {
        search("cost=200").andExpect(jsonPath("$.content[*].code", contains("REQ-000002")));
        search("cost=200&costOp=gt").andExpect(jsonPath("$.content[*].code", containsInAnyOrder("REQ-000003", "REQ-000004")));
        search("cost=200&costOp=lte").andExpect(jsonPath("$.content[*].code", containsInAnyOrder("REQ-000001", "REQ-000002")));
        search("from=2030-01-11T00:00:00&to=2030-01-12T23:59:00")
                .andExpect(jsonPath("$.content[*].code", containsInAnyOrder("REQ-000002", "REQ-000003")));
        search("costOp=gt").andExpect(status().isBadRequest());
        search("from=2030-02-01T00:00:00&to=2030-01-01T00:00:00").andExpect(status().isBadRequest());
        search("costOp=between&cost=1").andExpect(status().isBadRequest());
    }

    @Test
    void sortingAndPaging() throws Exception {
        search("sort=cost&direction=asc")
                .andExpect(jsonPath("$.content[*].code", contains("REQ-000001", "REQ-000002", "REQ-000003", "REQ-000004")));
        search("sort=startTime&direction=desc")
                .andExpect(jsonPath("$.content[0].code").value("REQ-000004"));
        search("sort=code&direction=asc&size=2&page=1")
                .andExpect(jsonPath("$.content[*].code", contains("REQ-000003", "REQ-000004")))
                .andExpect(jsonPath("$.totalPages").value(2));
        search("size=1000").andExpect(jsonPath("$.size").value(100));
        search("sort=owner").andExpect(status().isBadRequest());
        search("sort=cost&direction=up").andExpect(status().isBadRequest());
    }

    private ResultActions search(String query) throws Exception {
        return mvc.perform(get(java.net.URI.create("/api/requests?" + query)));
    }

    private static String enc(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private void insert(long id, String code, long owner, long vehicle, long spot, int status,
                        LocalDateTime start, String cost, boolean paid, boolean archived) {
        jdbc.update("insert into requests (id, uuid, code, user_id, vehicle_id, spot_id, status_id, start_time, end_time, "
                        + "cost, paid, archived, created_at, updated_at, version) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), 0)",
                id, UUID.randomUUID(), code, owner, vehicle, spot, status, start, start.plusHours(2),
                new java.math.BigDecimal(cost), paid, archived, start.minusDays(30).plusMinutes(id));
    }
}

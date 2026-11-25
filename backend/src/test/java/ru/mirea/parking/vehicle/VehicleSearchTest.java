package ru.mirea.parking.vehicle;

import org.junit.jupiter.api.AfterEach;
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
import ru.mirea.parking.TestDataCleaner;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Поиск, фильтры, сортировка и страницы GET /api/vehicles на H2. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "VIEWER")
class VehicleSearchTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        VehicleTestData.seed(jdbc);
        VehicleTestData.vehicle(jdbc, 1, 1, "А123ВС777", "Toyota", "Camry", 2018, false, "ACTIVE", false, "vip");
        VehicleTestData.vehicle(jdbc, 2, 1, "В045КМ177", "Lada", "Vesta", 2021, false, "BLOCKED", false);
        VehicleTestData.vehicle(jdbc, 3, 2, "Е900ОР50", "Tesla", "Model 3", 2023, true, "ACTIVE", false, "vip");
        VehicleTestData.vehicle(jdbc, 4, 2, "К300ТХ777", "Toyota", "Corolla", 2010, false, "DEREGISTERED", true);

        LocalDateTime now = LocalDateTime.now();
        // Camry: 4 заявки за 30 дней + отменённая и старая — HIGH; Vesta: одна — MEDIUM; Tesla — LOW.
        for (int i = 1; i <= 4; i++) {
            VehicleTestData.request(jdbc, i, 1, 3, now.minusDays(i));
        }
        VehicleTestData.request(jdbc, 5, 1, 4, now.minusDays(1));
        VehicleTestData.request(jdbc, 6, 1, 3, now.minusDays(45));
        VehicleTestData.request(jdbc, 7, 2, 4, now.minusDays(3));
        VehicleTestData.request(jdbc, 8, 2, 1, now.plusDays(3));
        VehicleTestData.request(jdbc, 9, 3, 4, now.minusDays(2));
    }

    @AfterEach
    void clean() {
        TestDataCleaner.clean(jdbc);
    }

    @Test
    void archivedHiddenByDefault() throws Exception {
        search().andExpect(jsonPath("$.totalElements").value(3));
        search("archived", "ALL").andExpect(jsonPath("$.totalElements").value(4));
        search("archived", "archived").andExpect(jsonPath("$.content[0].licensePlate").value("К300ТХ777"));
    }

    @Test
    void textSearchByPlateInLatinOrCyrillicAndByBrand() throws Exception {
        plates(search("q", "a123"), "А123ВС777");
        plates(search("q", "А123"), "А123ВС777");
        plates(search("q", "tesl"), "Е900ОР50");
        plates(search("q", "vest"), "В045КМ177");
    }

    @Test
    void filtersCombineWithAnd() throws Exception {
        plates(search("region", "777"), "А123ВС777");
        plates(search("status", "blocked"), "В045КМ177");
        plates(search("electric", "true"), "Е900ОР50");
        plates(search("tag", "VIP"), "А123ВС777", "Е900ОР50");
        plates(search("year", "2021", "yearOp", "gte"), "В045КМ177", "Е900ОР50");
        plates(search("tag", "vip", "electric", "false"), "А123ВС777");
    }

    @Test
    void activityFilterMatchesCalculatedLevel() throws Exception {
        plates(search("activity", "HIGH"), "А123ВС777");
        plates(search("activity", "medium"), "В045КМ177");
        plates(search("activity", "LOW"), "Е900ОР50");
        search("q", "a123").andExpect(jsonPath("$.content[0].activity.code").value("HIGH"))
                .andExpect(jsonPath("$.content[0].recentRequests").value(4));
    }

    @Test
    void sortByPlateUsesDigits() throws Exception {
        search("sort", "licensePlate", "direction", "asc")
                .andExpect(jsonPath("$.content[*].licensePlate", contains("В045КМ177", "А123ВС777", "Е900ОР50")));
        search("sort", "releaseYear", "direction", "desc")
                .andExpect(jsonPath("$.content[0].releaseYear").value(2023));
    }

    @Test
    void wrongParametersAre400() throws Exception {
        search("sort", "owner").andExpect(status().isBadRequest());
        search("yearOp", "gt").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("yearOp"));
        search("activity", "SUPER").andExpect(status().isBadRequest());
    }

    @Test
    void pageSizeIsCappedAt100() throws Exception {
        search("size", "1000").andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void parkedDuringPeriod() throws Exception {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        mvc.perform(get("/api/vehicles/parked")
                        .param("from", now.minusDays(2).minusHours(1).toString())
                        .param("to", now.minusDays(1).plusHours(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].licensePlate", contains("А123ВС777")));
        mvc.perform(get("/api/vehicles/parked").param("from", now.toString()).param("to", now.minusDays(1).toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void parkedPeriodFilterInList() throws Exception {
        LocalDateTime start = LocalDateTime.now().withNano(0).minusDays(1);
        plates(search("parkedFrom", start.toString(), "parkedTo", start.plusHours(3).toString()), "А123ВС777");
        search("parkedFrom", start.toString()).andExpect(status().isBadRequest());
    }

    private ResultActions search(String... params) throws Exception {
        var request = get("/api/vehicles");
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mvc.perform(request);
    }

    private static void plates(ResultActions result, String... expected) throws Exception {
        result.andExpect(status().isOk()).andExpect(jsonPath("$.content[*].licensePlate", containsInAnyOrder(expected)));
    }
}

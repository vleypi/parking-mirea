package ru.mirea.parking.spot;

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

/** Поиск, фильтры, сортировка и страницы GET /api/spots, свободные места на период — на H2. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "VIEWER")
class SpotSearchTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    private LocalDateTime now;

    @BeforeEach
    void seed() {
        SpotTestData.seed(jdbc);
        SpotTestData.spot(jdbc, 1, 1, 1, "100.00", "A", "У въезда", "AVAILABLE", false, false, "у входа");
        SpotTestData.spot(jdbc, 2, 2, 2, "50.00", "A", null, "AVAILABLE", true, false);
        SpotTestData.spot(jdbc, 3, 5, 3, "150.00", "B", "Зарядка 22 кВт", "AVAILABLE", true, false, "у входа");
        SpotTestData.spot(jdbc, 4, 10, 1, "120.00", "C", null, "MAINTENANCE", false, false);
        SpotTestData.spot(jdbc, 5, 11, 1, "90.00", "C", null, "AVAILABLE", false, true);

        now = LocalDateTime.now().withNano(0);
        // Место 1 занято сейчас подтверждённой заявкой; на место 2 есть только отменённая и завершённая.
        SpotTestData.request(jdbc, 1, 1, 2, now.minusHours(1), now.plusHours(1));
        SpotTestData.request(jdbc, 2, 2, 4, now.minusHours(1), now.plusHours(1));
        SpotTestData.request(jdbc, 3, 2, 3, now.minusDays(1));
        // Место 5 в архиве: не мешает остальным; на место 3 — новая заявка завтра.
        SpotTestData.request(jdbc, 4, 3, 1, now.plusDays(1), now.plusDays(1).plusHours(3));
    }

    @AfterEach
    void clean() {
        TestDataCleaner.clean(jdbc);
    }

    @Test
    void archivedHiddenByDefault() throws Exception {
        search().andExpect(jsonPath("$.totalElements").value(4));
        search("archived", "all").andExpect(jsonPath("$.totalElements").value(5));
        numbers(search("archived", "ARCHIVED"), 11);
    }

    @Test
    void textSearchByNumberZoneOrDescription() throws Exception {
        numbers(search("q", "5"), 5);
        numbers(search("q", "a"), 1, 2);
        numbers(search("q", "заряд"), 5);
    }

    @Test
    void filtersCombineWithAnd() throws Exception {
        numbers(search("spotTypeId", "1"), 1, 10);
        numbers(search("status", "maintenance"), 10);
        numbers(search("covered", "true"), 2, 5);
        numbers(search("tag", "У ВХОДА"), 1, 5);
        numbers(search("rate", "100", "rateOp", "gt"), 5, 10);
        numbers(search("rate", "100", "rateOp", "lte", "covered", "true"), 2);
    }

    @Test
    void availabilityFilterMatchesCalculatedValue() throws Exception {
        numbers(search("availability", "BUSY_NOW"), 1);
        numbers(search("availability", "free_now"), 2, 5);
        numbers(search("availability", "UNAVAILABLE"), 10);
        search("q", "1").andExpect(jsonPath("$.content[0].availability.code").value("BUSY_NOW"));
    }

    @Test
    void sortByRateAndZone() throws Exception {
        search("sort", "hourlyRate", "direction", "desc")
                .andExpect(jsonPath("$.content[*].spotNumber", contains(5, 10, 1, 2)));
        search("sort", "zone", "direction", "asc")
                .andExpect(jsonPath("$.content[*].spotNumber", contains(1, 2, 5, 10)));
        search().andExpect(jsonPath("$.content[*].spotNumber", contains(1, 2, 5, 10)));
    }

    @Test
    void wrongParametersAre400() throws Exception {
        search("sort", "owner").andExpect(status().isBadRequest());
        search("rateOp", "gt").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("rateOp"));
        search("availability", "SOMETIMES").andExpect(status().isBadRequest());
    }

    @Test
    void pageSizeIsCappedAt100() throws Exception {
        search("size", "1000").andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void freeSpotsForPeriod() throws Exception {
        LocalDateTime from = now.plusDays(1).plusHours(1);
        mvc.perform(get("/api/spots/free").param("from", from.toString()).param("to", from.plusHours(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].spotNumber", contains(1, 2)));
        // Период кончается ровно в начале заявки на место 5 — место свободно.
        LocalDateTime end = now.plusDays(1);
        mvc.perform(get("/api/spots/free").param("from", end.minusHours(1).toString()).param("to", end.toString()))
                .andExpect(jsonPath("$[*].spotNumber", contains(1, 2, 5)));
        mvc.perform(get("/api/spots/free").param("from", end.toString()).param("to", end.minusHours(1).toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void freePeriodFilterInList() throws Exception {
        LocalDateTime from = now.plusHours(2);
        numbers(search("freeFrom", from.toString(), "freeTo", from.plusHours(1).toString()), 1, 2, 5);
        search("freeFrom", from.toString()).andExpect(status().isBadRequest());
    }

    private ResultActions search(String... params) throws Exception {
        var request = get("/api/spots");
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        return mvc.perform(request);
    }

    private static void numbers(ResultActions result, Integer... expected) throws Exception {
        result.andExpect(status().isOk()).andExpect(jsonPath("$.content[*].spotNumber", containsInAnyOrder(expected)));
    }
}

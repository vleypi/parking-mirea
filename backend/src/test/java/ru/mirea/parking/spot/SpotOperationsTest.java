package ru.mirea.parking.spot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.mirea.parking.TestDataCleaner;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Массовые операции, CSV, аналитика и окончательное удаление мест. */
@SpringBootTest(properties = "app.permanent-delete-enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "OPERATOR")
class SpotOperationsTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper json;

    private UUID one;
    private UUID two;
    private UUID five;

    @BeforeEach
    void seed() {
        SpotTestData.seed(jdbc);
        one = SpotTestData.spot(jdbc, 1, 1, 1, "100.00", "A", null, "AVAILABLE", false, false);
        two = SpotTestData.spot(jdbc, 2, 2, 2, "50.00", "A", null, "AVAILABLE", true, false);
        five = SpotTestData.spot(jdbc, 3, 5, 3, "150.00", "B", null, "CLOSED", true, true);
    }

    @AfterEach
    void clean() {
        TestDataCleaner.clean(jdbc);
    }

    @Test
    void bulkArchiveSkipsSpotWithFutureRequest() throws Exception {
        SpotTestData.request(jdbc, 1, 1, 2, LocalDateTime.now().plusDays(1));
        UUID unknown = UUID.randomUUID();
        bulk("""
                {"action":"ARCHIVE","ids":["%s","%s","%s","%s"]}""".formatted(one, two, five, unknown))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.succeeded[*]", containsInAnyOrder(two.toString())))
                .andExpect(jsonPath("$.failed.length()").value(3))
                .andExpect(jsonPath("$.failed[?(@.id == '%s')].reason".formatted(one), hasItem(containsString("ещё не закончилась"))))
                .andExpect(jsonPath("$.failed[?(@.id == '%s')].reason".formatted(five)).value("Место 5 уже в архиве"))
                .andExpect(jsonPath("$.failed[?(@.id == '%s')].reason".formatted(unknown)).value("Место " + unknown + " не найдено"));
    }

    @Test
    void bulkMaintenanceWithAudit() throws Exception {
        jdbc.update("delete from audit_event");
        bulk("""
                {"action":"CHANGE_STATUS","status":"MAINTENANCE","ids":["%s","%s"]}""".formatted(one, two))
                .andExpect(jsonPath("$.succeeded.length()").value(2));
        mvc.perform(get("/api/spots").param("status", "MAINTENANCE")).andExpect(jsonPath("$.totalElements").value(2));
        Integer audit = jdbc.queryForObject("select count(*) from audit_event where entity_type = 'SPOT'", Integer.class);
        assertThat(audit).isEqualTo(2);
    }

    @Test
    void exportUsesListFilters() throws Exception {
        String csv = mvc.perform(get("/api/spots/export").param("covered", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("номер;тип;тип_название;тариф").contains("2;DISABLED;").doesNotContain("\n1;STANDARD");
    }

    @Test
    void importPreviewThenUpsertByNumber() throws Exception {
        jdbc.update("update spot_types set active = false where id = 2");
        String file = """
                номер;тип;тариф;зона;статус;крытое;теги
                1;standard;110,50;A;На обслуживании;да;ремонт
                7;ELECTRIC;200;B;;нет;
                8;VIP;100;;;;
                9;DISABLED;100;;;;
                -3;STANDARD;100;;;;
                10;STANDARD;-1;;;;
                """;
        JsonNode preview = json.readTree(mvc.perform(multipart("/api/spots/import/preview")
                        .file(new MockMultipartFile("file", "s.csv", "text/csv", file.getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(6))
                .andExpect(jsonPath("$.validRows").value(2))
                .andExpect(jsonPath("$.errors[?(@.row == 4)].message", hasItem(containsString("Неизвестный тип"))))
                .andExpect(jsonPath("$.errors[?(@.row == 5)].message", hasItem(containsString("отключён"))))
                .andReturn().getResponse().getContentAsString());
        assertThat(jdbc.queryForObject("select count(*) from spots", Integer.class)).isEqualTo(3);

        mvc.perform(post("/api/spots/import/apply").contentType(MediaType.APPLICATION_JSON).content("""
                        {"importToken":"%s","mode":"UPSERT"}""".formatted(preview.get("importToken").asText())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.updated").value(1));
        mvc.perform(get("/api/spots/" + one))
                .andExpect(jsonPath("$.hourlyRate").value(110.50))
                .andExpect(jsonPath("$.status.code").value("MAINTENANCE"))
                .andExpect(jsonPath("$.covered").value(true));
        mvc.perform(get("/api/spots").param("q", "7")).andExpect(jsonPath("$.content[0].spotType.code").value("ELECTRIC"));
    }

    @Test
    void analyticsCountsAllWithoutArchivedFilter() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        SpotTestData.request(jdbc, 1, 1, 1, now.minusMinutes(30), now.plusMinutes(30));
        mvc.perform(get("/api/analytics/spots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.active").value(2))
                .andExpect(jsonPath("$.archived").value(1))
                .andExpect(jsonPath("$.covered").value(2))
                .andExpect(jsonPath("$.averageHourlyRate").value(100.00))
                .andExpect(jsonPath("$.byType['Стандартное']").value(1))
                .andExpect(jsonPath("$.byStatus.CLOSED").value(1))
                .andExpect(jsonPath("$.byAvailability.BUSY_NOW").value(1))
                .andExpect(jsonPath("$.byAvailability.FREE_NOW").value(1))
                .andExpect(jsonPath("$.byAvailability.UNAVAILABLE").value(1));
        mvc.perform(get("/api/analytics/summary"))
                .andExpect(jsonPath("$.spots.total").value(3))
                .andExpect(jsonPath("$.vehicles.total").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void permanentDeleteOnlyWithoutRequests() throws Exception {
        SpotTestData.request(jdbc, 1, 1, 3, LocalDateTime.now().minusDays(1));
        mvc.perform(delete("/api/spots/" + one + "/permanent"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("есть заявки")));
        mvc.perform(delete("/api/spots/" + two + "/permanent")).andExpect(status().isNoContent());
        mvc.perform(get("/api/spots/" + two)).andExpect(status().isNotFound());
    }

    private ResultActions bulk(String body) throws Exception {
        return mvc.perform(post("/api/spots/bulk").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}

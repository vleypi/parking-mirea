package ru.mirea.parking.vehicle;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Массовые операции, CSV, аналитика и окончательное удаление автомобилей. */
@SpringBootTest(properties = "app.permanent-delete-enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "OPERATOR")
class VehicleOperationsTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper json;

    private UUID camry;
    private UUID vesta;
    private UUID oldCorolla;

    @BeforeEach
    void seed() {
        VehicleTestData.seed(jdbc);
        camry = VehicleTestData.vehicle(jdbc, 1, 1, "А123ВС777", "Toyota", "Camry", 2018, false, "ACTIVE", false, "vip");
        vesta = VehicleTestData.vehicle(jdbc, 2, 1, "В045КМ177", "Lada", "Vesta", 2020, false, "ACTIVE", false);
        oldCorolla = VehicleTestData.vehicle(jdbc, 3, 2, "К300ТХ777", "Toyota", "Corolla", 2010, true, "BLOCKED", true);
    }

    @AfterEach
    void clean() {
        TestDataCleaner.clean(jdbc);
    }

    @Test
    void bulkArchiveReportsEachRecord() throws Exception {
        UUID unknown = UUID.randomUUID();
        bulk("""
                {"action":"ARCHIVE","ids":["%s","%s","%s","%s"]}""".formatted(camry, vesta, oldCorolla, unknown))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.succeeded[*]", containsInAnyOrder(camry.toString(), vesta.toString())))
                .andExpect(jsonPath("$.failed[?(@.id == '%s')].reason".formatted(oldCorolla))
                        .value("Автомобиль К300ТХ777 уже в архиве"))
                .andExpect(jsonPath("$.failed[?(@.id == '%s')].reason".formatted(unknown))
                        .value("Автомобиль " + unknown + " не найден"));
    }

    @Test
    void bulkStatusAndTags() throws Exception {
        bulk("""
                {"action":"CHANGE_STATUS","ids":["%s"]}""".formatted(camry))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
        bulk("""
                {"action":"CHANGE_STATUS","status":"BLOCKED","ids":["%s","%s"]}""".formatted(camry, vesta))
                .andExpect(jsonPath("$.succeeded.length()").value(2));
        bulk("""
                {"action":"ADD_TAG","tag":" Ночь ","ids":["%s"]}""".formatted(vesta))
                .andExpect(jsonPath("$.succeeded.length()").value(1));
        mvc.perform(get("/api/vehicles/" + vesta))
                .andExpect(jsonPath("$.status.code").value("BLOCKED"))
                .andExpect(jsonPath("$.tags[0]").value("ночь"));
    }

    @Test
    void exportUsesListFilters() throws Exception {
        String csv = mvc.perform(get("/api/vehicles/export").param("q", "camry"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("гос_номер;марка").contains("А123ВС777;Toyota;Camry").doesNotContain("Vesta");
    }

    @Test
    void importPreviewThenUpsert() throws Exception {
        String file = """
                гос_номер;марка;модель;год;статус;телефон_владельца;теги
                a123bc777;Toyota;Camry 2.5;2018;Допущен;+7-900-100-00-01;vip
                Е555ЕЕ99;Kia;Rio;2022;;89001000002;такси
                bad;Kia;Rio;2022;;+7-900-100-00-02;
                Е556ЕЕ99;Kia;Rio;2022;;+7-900-100-00-03;
                """;
        JsonNode preview = json.readTree(mvc.perform(multipart("/api/vehicles/import/preview")
                        .file(new MockMultipartFile("file", "v.csv", "text/csv", file.getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(4))
                .andExpect(jsonPath("$.validRows").value(2))
                .andExpect(jsonPath("$.errors[?(@.row == 4)].message").value(org.hamcrest.Matchers.hasItem(containsString("гос. номер"))))
                .andExpect(jsonPath("$.errors[?(@.row == 5)].message").value(org.hamcrest.Matchers.hasItem(containsString("в архиве"))))
                .andReturn().getResponse().getContentAsString());
        assertThat(jdbc.queryForObject("select count(*) from vehicles", Integer.class)).isEqualTo(3);

        mvc.perform(post("/api/vehicles/import/apply").contentType(MediaType.APPLICATION_JSON).content("""
                        {"importToken":"%s","mode":"UPSERT"}""".formatted(preview.get("importToken").asText())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.updated").value(1));
        mvc.perform(get("/api/vehicles/" + camry)).andExpect(jsonPath("$.model").value("Camry 2.5"));
        mvc.perform(get("/api/vehicles").param("q", "Е555ЕЕ99"))
                .andExpect(jsonPath("$.content[0].owner.name").value("Мария Петрова"))
                .andExpect(jsonPath("$.content[0].tags[0]").value("такси"));
    }

    @Test
    void analyticsCountsAllWithoutArchivedFilter() throws Exception {
        VehicleTestData.request(jdbc, 1, 1, 1, LocalDateTime.now().minusDays(1));
        mvc.perform(get("/api/analytics/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.active").value(2))
                .andExpect(jsonPath("$.archived").value(1))
                .andExpect(jsonPath("$.electric").value(1))
                .andExpect(jsonPath("$.averageReleaseYear").value(2016.0))
                .andExpect(jsonPath("$.byBrand.Toyota").value(2))
                .andExpect(jsonPath("$.byStatus.BLOCKED").value(1))
                .andExpect(jsonPath("$.byActivity.MEDIUM").value(1))
                .andExpect(jsonPath("$.byActivity.LOW").value(2));
        mvc.perform(get("/api/analytics/summary"))
                .andExpect(jsonPath("$.vehicles.total").value(3));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void permanentDeleteOnlyWithoutRequests() throws Exception {
        VehicleTestData.request(jdbc, 1, 1, 3, LocalDateTime.now().minusDays(1));
        mvc.perform(delete("/api/vehicles/" + camry + "/permanent"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("есть заявки")));
        mvc.perform(delete("/api/vehicles/" + vesta + "/permanent")).andExpect(status().isNoContent());
        mvc.perform(get("/api/vehicles/" + vesta)).andExpect(status().isNotFound());
    }

    private ResultActions bulk(String body) throws Exception {
        return mvc.perform(post("/api/vehicles/bulk").contentType(MediaType.APPLICATION_JSON).content(body));
    }
}

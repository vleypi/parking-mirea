package ru.mirea.parking.request;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Экспорт и импорт заявок в CSV. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "OPERATOR")
class RequestCsvTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper json;

    @BeforeEach
    void seed() {
        RequestTestData.seed(jdbc);
        jdbc.update("alter table requests alter column id restart with 1000");
    }

    @Test
    void exportUsesListFilters() throws Exception {
        String csv = mvc.perform(get("/api/requests/export").param("tag", "ночь"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv).contains("код;телефон_владельца;гос_номер;номер_места;начало;окончание")
                .contains("REQ-000003;'+7-900-100-00-02;В456КМ177;5;2030-01-12 09:00;2030-01-12 11:00")
                .contains("REQ-000004")
                .doesNotContain("REQ-000001");
    }

    @Test
    void importPreviewThenUpsert() throws Exception {
        String file = """
                код;телефон_владельца;гос_номер;номер_места;начало;окончание;статус;оплачена;теги
                ;89001000001;а123вс777;1;2030-02-01 10:00;2030-02-01 12:00;Подтверждена;да;импорт
                REQ-000001;+7-900-100-00-01;А123ВС777;1;2030-01-10 09:00;2030-01-10 11:00;NEW;да;vip,оплачено
                ;+7-900-100-00-02;А123ВС777;1;2030-03-01 10:00;2030-03-01 12:00;;;
                REQ-999999;+7-900-100-00-01;А123ВС777;1;2030-03-02 10:00;2030-03-02 12:00;;;
                ;+7-900-100-00-01;А123ВС777;1;2030-03-03 12:00;2030-03-03 10:00;;;
                """;
        ResultActions preview = preview(file)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(5))
                .andExpect(jsonPath("$.validRows").value(3))
                .andExpect(jsonPath("$.errors[?(@.row == 4)].message").value(hasItem(containsString("не принадлежит"))))
                .andExpect(jsonPath("$.errors[?(@.row == 6)].message").value(hasItem(containsString("позже начала"))));
        assertThat(jdbc.queryForObject("select count(*) from requests", Integer.class)).isEqualTo(5);

        mvc.perform(post("/api/requests/import/apply").contentType(MediaType.APPLICATION_JSON).content("""
                        {"importToken":"%s","mode":"UPSERT"}""".formatted(token(preview))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(1))
                .andExpect(jsonPath("$.updated").value(1))
                .andExpect(jsonPath("$.errors[?(@.row == 5)].message").value(hasItem(containsString("не найдена"))));

        assertThat(jdbc.queryForObject("select paid from requests where code = 'REQ-000001'", Boolean.class)).isTrue();
        mvc.perform(get("/api/requests").param("tag", "импорт"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status.code").value("CONFIRMED"))
                .andExpect(jsonPath("$.content[0].cost").value(200.00));
    }

    @Test
    void createOnlySkipsExistingCodes() throws Exception {
        String file = """
                код;телефон_владельца;гос_номер;номер_места;начало;окончание;оплачена
                REQ-000001;+7-900-100-00-01;А123ВС777;1;2030-01-10 09:00;2030-01-10 11:00;да
                """;
        String token = token(preview(file));
        mvc.perform(post("/api/requests/import/apply").contentType(MediaType.APPLICATION_JSON).content("""
                        {"importToken":"%s","mode":"CREATE_ONLY"}""".formatted(token)))
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.updated").value(0));
        assertThat(jdbc.queryForObject("select paid from requests where code = 'REQ-000001'", Boolean.class)).isFalse();
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerCannotImport() throws Exception {
        mvc.perform(multipart("/api/requests/import/preview")
                        .file(new MockMultipartFile("file", "r.csv", "text/csv", "код\n".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isForbidden());
    }

    private ResultActions preview(String file) throws Exception {
        return mvc.perform(multipart("/api/requests/import/preview")
                .file(new MockMultipartFile("file", "r.csv", "text/csv", file.getBytes(StandardCharsets.UTF_8))));
    }

    private String token(ResultActions preview) throws Exception {
        return json.readTree(preview.andReturn().getResponse().getContentAsString()).get("importToken").asText();
    }
}

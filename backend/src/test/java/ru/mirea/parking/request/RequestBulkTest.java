package ru.mirea.parking.request;

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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "OPERATOR")
class RequestBulkTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper json;

    @BeforeEach
    void seed() {
        RequestTestData.seed(jdbc);
    }

    @Test
    void fiveRequestsWithTwoInvalidTransitionsGiveThreeSuccessesAndTwoErrors() throws Exception {
        List<String> ids = List.of(uuid(1), uuid(2), uuid(3), uuid(4), uuid(5));

        bulk(ids, "CHANGE_STATUS", "CONFIRMED", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.succeeded", containsInAnyOrder(uuid(1), uuid(2), uuid(3))))
                .andExpect(jsonPath("$.failed.length()").value(2))
                .andExpect(jsonPath("$.failed[0].id").value(uuid(4)))
                .andExpect(jsonPath("$.failed[0].reason").value("Заявку в статусе «Завершена» изменить нельзя: редактируются только новые и подтверждённые заявки"))
                .andExpect(jsonPath("$.failed[1].id").value(uuid(5)))
                .andExpect(jsonPath("$.failed[1].reason").value("Архивную заявку изменить нельзя"));

        assertThat(jdbc.queryForList("select status_id from requests where id in (1, 2, 3)", Integer.class)).containsOnly(2);
        assertThat(jdbc.queryForObject("select status_id from requests where id = 4", Integer.class)).isEqualTo(3);
    }

    @Test
    void unknownIdDoesNotCancelOthers() throws Exception {
        String unknown = UUID.randomUUID().toString();
        bulk(List.of(uuid(1), unknown), "ARCHIVE", null, null)
                .andExpect(jsonPath("$.succeeded[0]").value(uuid(1)))
                .andExpect(jsonPath("$.failed[0].id").value(unknown))
                .andExpect(jsonPath("$.failed[0].reason").value("Заявка " + unknown + " не найдена"));
        assertThat(jdbc.queryForObject("select archived from requests where id = 1", Boolean.class)).isTrue();
    }

    @Test
    void archiveAndRestore() throws Exception {
        bulk(List.of(uuid(1), uuid(5)), "ARCHIVE", null, null)
                .andExpect(jsonPath("$.succeeded[0]").value(uuid(1)))
                .andExpect(jsonPath("$.failed[0].id").value(uuid(5)));
        bulk(List.of(uuid(1), uuid(5), uuid(2)), "RESTORE", null, null)
                .andExpect(jsonPath("$.succeeded", containsInAnyOrder(uuid(1), uuid(5))))
                .andExpect(jsonPath("$.failed[0].id").value(uuid(2)));
    }

    @Test
    void addAndRemoveTag() throws Exception {
        bulk(List.of(uuid(1), uuid(2)), "ADD_TAG", null, " Ночь ").andExpect(jsonPath("$.succeeded.length()").value(2));
        assertThat(jdbc.queryForList("select request_id from request_tag where tag = 'ночь' order by request_id", Long.class))
                .containsExactly(1L, 2L, 3L, 4L);

        bulk(List.of(uuid(1), uuid(3)), "REMOVE_TAG", null, "vip").andExpect(jsonPath("$.succeeded.length()").value(2));
        assertThat(jdbc.queryForObject("select count(*) from request_tag where tag = 'vip'", Long.class)).isZero();
    }

    @Test
    void validationOfThePackage() throws Exception {
        bulk(List.of(), "ARCHIVE", null, null).andExpect(status().isBadRequest());
        bulk(Collections.nCopies(201, uuid(1)), "ARCHIVE", null, null).andExpect(status().isBadRequest());
        bulk(List.of(uuid(1)), "CHANGE_STATUS", null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Для смены статуса укажите status"));
        bulk(List.of(uuid(1)), "ADD_TAG", null, " ").andExpect(status().isBadRequest());
        bulk(List.of(uuid(1)), "DELETE_ALL", null, null).andExpect(status().isBadRequest());
    }

    @Test
    void unknownStatusIsReportedPerRequest() throws Exception {
        bulk(List.of(uuid(1)), "CHANGE_STATUS", "DONE", null)
                .andExpect(jsonPath("$.failed[0].reason").value("Неизвестный статус DONE"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerGets403() throws Exception {
        bulk(List.of(uuid(1)), "ARCHIVE", null, null).andExpect(status().isForbidden());
    }

    private ResultActions bulk(List<String> ids, String action, String status, String tag) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("ids", ids);
        body.put("action", action);
        if (status != null) body.put("status", status);
        if (tag != null) body.put("tag", tag);
        return mvc.perform(post("/api/requests/bulk").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private String uuid(int id) {
        return jdbc.queryForObject("select uuid from requests where id = ?", UUID.class, id).toString();
    }
}

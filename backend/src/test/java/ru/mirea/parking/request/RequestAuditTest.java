package ru.mirea.parking.request;

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
import ru.mirea.parking.domain.AuditEntityType;
import ru.mirea.parking.domain.AuditEvent;
import ru.mirea.parking.domain.AuditEventType;
import ru.mirea.parking.repository.AuditEventRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Архивирование, восстановление, удаление и массовые операции заявок попадают в журнал аудита. */
@SpringBootTest(properties = "app.permanent-delete-enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = {"OPERATOR", "ADMIN"})
class RequestAuditTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private AuditEventRepository events;

    private UUID first;
    private UUID third;

    @BeforeEach
    void seed() {
        events.deleteAll();
        RequestTestData.seed(jdbc);
        first = jdbc.queryForObject("select uuid from requests where id = 1", UUID.class);
        third = jdbc.queryForObject("select uuid from requests where id = 3", UUID.class);
    }

    @Test
    void archiveRestoreAndDeleteAreRecorded() throws Exception {
        mvc.perform(delete("/api/requests/" + first)).andExpect(status().isOk());
        mvc.perform(post("/api/requests/" + first + "/restore")).andExpect(status().isOk());
        mvc.perform(delete("/api/requests/" + first + "/permanent")).andExpect(status().isNoContent());

        List<AuditEvent> recorded = events.findAll();
        assertThat(recorded).allMatch(e -> e.getEntityType() == AuditEntityType.REQUEST && first.equals(e.getEntityId()));
        assertThat(recorded).extracting(AuditEvent::getEventType)
                .containsExactlyInAnyOrder(AuditEventType.ARCHIVE, AuditEventType.RESTORE, AuditEventType.DELETE);
        assertThat(recorded).extracting(AuditEvent::getSummary)
                .contains("Заявка REQ-000001 перенесена в архив", "Заявка REQ-000001 удалена окончательно");
    }

    @Test
    void bulkRecordsOnlySucceededItems() throws Exception {
        mvc.perform(post("/api/requests/bulk").contentType(MediaType.APPLICATION_JSON).content("""
                        {"ids":["%s","%s","%s"],"action":"ADD_TAG","tag":"Проверка"}""".formatted(first, third, UUID.randomUUID())))
                .andExpect(status().isOk());

        List<AuditEvent> recorded = events.findAll();
        assertThat(recorded).hasSize(2).allMatch(e -> e.getEventType() == AuditEventType.UPDATE);
        assertThat(recorded).extracting(AuditEvent::getSummary)
                .containsExactlyInAnyOrder("Заявке REQ-000001 добавлен тег «проверка»", "Заявке REQ-000003 добавлен тег «проверка»");
    }

    @Test
    void failedActionLeavesNoEvent() throws Exception {
        mvc.perform(post("/api/requests/" + first + "/restore")).andExpect(status().isConflict());
        assertThat(events.findAll()).isEmpty();
    }
}

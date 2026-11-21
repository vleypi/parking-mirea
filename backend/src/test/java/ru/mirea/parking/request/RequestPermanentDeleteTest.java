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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.permanent-delete-enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "ADMIN")
class RequestPermanentDeleteTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        RequestTestData.seed(jdbc);
    }

    @Test
    void adminDeletesRequestWithTagsWhenFlagIsOn() throws Exception {
        String id = jdbc.queryForObject("select uuid from requests where id = 3", UUID.class).toString();

        mvc.perform(delete("/api/requests/" + id + "/permanent")).andExpect(status().isNoContent());

        mvc.perform(get("/api/requests/" + id)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from request_tag where request_id = 3", Long.class)).isZero();
        mvc.perform(delete("/api/requests/" + id + "/permanent")).andExpect(status().isNotFound());
    }
}

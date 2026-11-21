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

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        RequestTestData.seed(jdbc);
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
}

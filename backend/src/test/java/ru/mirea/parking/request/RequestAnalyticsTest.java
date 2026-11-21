package ru.mirea.parking.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.Iterator;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "VIEWER")
class RequestAnalyticsTest {

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
    void summaryOfAllRequests() throws Exception {
        JsonNode r = summary("");
        assertThat(r.get("total").asLong()).isEqualTo(5);
        assertThat(r.get("active").asLong()).isEqualTo(4);
        assertThat(r.get("archived").asLong()).isEqualTo(1);
        assertThat(r.get("paid").asLong()).isEqualTo(2);
        assertThat(r.get("unpaid").asLong()).isEqualTo(3);
        assertThat(r.get("averageCost").decimalValue()).isEqualByComparingTo("220.00");
        assertThat(r.get("byStatus").toString()).isEqualTo("{\"NEW\":2,\"CONFIRMED\":1,\"COMPLETED\":1,\"CANCELLED\":1}");
        assertThat(r.get("bySpotType").toString()).isEqualTo("{\"STANDARD\":2,\"DISABLED\":0,\"ELECTRIC\":3}");
        assertThat(r.get("byPriority").toString()).isEqualTo("{\"HIGH\":0,\"MEDIUM\":0,\"LOW\":5}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "archived=ACTIVE", "status=NEW", "spotType=ELECTRIC", "q=иван", "tag=vip",
            "paid=true", "cost=200&costOp=gte", "from=2030-01-11T00:00:00&to=2030-01-13T00:00:00"})
    void numbersMatchListWithSameConditions(String condition) throws Exception {
        String archivedDefault = condition.contains("archived=") ? "" : "archived=ALL";
        String listQuery = join(condition, archivedDefault);
        JsonNode summary = summary(condition);

        assertThat(summary.get("total").asLong()).isEqualTo(listTotal(listQuery));
        assertThat(summary.get("paid").asLong()).isEqualTo(listTotal(join(listQuery, "paid=true")));

        assertGroupMatchesList(summary.get("byStatus"), listQuery, "status");
        assertGroupMatchesList(summary.get("bySpotType"), listQuery, "spotType");
        assertGroupMatchesList(summary.get("byPriority"), listQuery, "priority");
    }

    private void assertGroupMatchesList(JsonNode group, String listQuery, String param) throws Exception {
        long sum = 0;
        for (Iterator<Map.Entry<String, JsonNode>> it = group.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            if (listQuery.contains(param + "=")) {
                sum += entry.getValue().asLong();
                continue;
            }
            assertThat(entry.getValue().asLong())
                    .as("%s=%s при условии [%s]", param, entry.getKey(), listQuery)
                    .isEqualTo(listTotal(join(listQuery, param + "=" + entry.getKey())));
            sum += entry.getValue().asLong();
        }
        assertThat(sum).isEqualTo(listTotal(listQuery));
    }

    private JsonNode summary(String query) throws Exception {
        String body = mvc.perform(get(URI.create("/api/analytics/summary?" + query)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("requests");
    }

    private long listTotal(String query) throws Exception {
        String body = mvc.perform(get(URI.create("/api/requests?" + query)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("totalElements").asLong();
    }

    private static String join(String a, String b) {
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        return a + "&" + b;
    }
}

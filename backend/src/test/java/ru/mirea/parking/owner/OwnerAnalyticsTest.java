package ru.mirea.parking.owner;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "VIEWER")
class OwnerAnalyticsTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper json;

    @BeforeEach
    void seed() {
        OwnerTestData.clean(jdbc);
        OwnerTestData.insert(jdbc, 1, "Иван Иванов", "+7-900-100-00-01", 1, "ACTIVE", "0", true, false, "vip");
        OwnerTestData.insert(jdbc, 2, "Мария Петрова", "+7-900-100-00-02", 2, "ACTIVE", "15", false, false);
        OwnerTestData.insert(jdbc, 3, "Пётр Сидоров", "+7-900-100-00-03", 2, "BLOCKED", "30", true, false, "vip");
        OwnerTestData.insert(jdbc, 4, "Анна Архивная", "+7-900-100-00-04", 1, "ACTIVE", "10", true, true);
    }

    @Test
    void summaryContainsOwnerBlock() throws Exception {
        JsonNode owners = read("/api/analytics/summary").get("owners");

        assertThat(owners.get("total").asLong()).isEqualTo(4);
        assertThat(owners.get("active").asLong()).isEqualTo(3);
        assertThat(owners.get("archived").asLong()).isEqualTo(1);
        assertThat(owners.get("notificationsEnabled").asLong()).isEqualTo(3);
        assertThat(owners.get("averageDiscount").decimalValue()).isEqualByComparingTo("13.75");
        assertThat(owners.get("byCategory").get("REGULAR").asLong()).isEqualTo(2);
        assertThat(owners.get("byCategory").get("RESIDENT").asLong()).isEqualTo(2);
        assertThat(owners.get("byCategory").get("EMPLOYEE").asLong()).isZero();
        assertThat(owners.get("byStatus").get("BLOCKED").asLong()).isEqualTo(1);
        assertThat(owners.get("byLoyalty").get("NEW").asLong()).isEqualTo(4);
        assertThat(owners.get("byLoyalty").get("VIP").asLong()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"archived=ACTIVE", "archived=ACTIVE&tag=vip", "archived=ALL&categoryId=2",
            "archived=ACTIVE&status=BLOCKED", "archived=ALL&discount=10&discountOp=gte", "archived=ARCHIVED"})
    void numbersMatchListWithSameConditions(String condition) throws Exception {
        JsonNode stats = read("/api/analytics/owners?" + condition);
        long listTotal = read("/api/owners?" + condition).get("totalElements").asLong();
        long listNotifications = read("/api/owners?" + condition + "&notifications=true").get("totalElements").asLong();

        assertThat(stats.get("total").asLong()).isEqualTo(listTotal);
        assertThat(stats.get("notificationsEnabled").asLong()).isEqualTo(listNotifications);
        long byCategorySum = 0;
        for (JsonNode count : stats.get("byCategory")) {
            byCategorySum += count.asLong();
        }
        assertThat(byCategorySum).isEqualTo(listTotal);
    }

    @Test
    void emptyResultHasZeroAverage() throws Exception {
        JsonNode stats = read("/api/analytics/owners?q=nobody");
        assertThat(stats.get("total").asLong()).isZero();
        assertThat(stats.get("averageDiscount").decimalValue()).isEqualByComparingTo("0");
    }

    private JsonNode read(String url) throws Exception {
        String body = mvc.perform(get(URI.create(url))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }
}

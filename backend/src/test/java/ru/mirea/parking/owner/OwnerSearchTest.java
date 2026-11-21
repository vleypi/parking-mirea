package ru.mirea.parking.owner;

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


import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Тест фильтра владельцев на H2: каждое условие отдельно и вместе. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "VIEWER")
class OwnerSearchTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        OwnerTestData.clean(jdbc);
        // id, имя, телефон, категория, статус, скидка, уведомления, архив, теги. Дата регистрации = 2026-01-15 + id дней.
        OwnerTestData.insert(jdbc, 1, "Иван Иванов", "+7-900-100-00-01", 1, "ACTIVE", "0", true, false, "vip");
        OwnerTestData.insert(jdbc, 2, "Мария O'Нил_100%", "+7-912-345-67-89", 2, "ACTIVE", "15", false, false, "ночь");
        OwnerTestData.insert(jdbc, 3, "Пётр Сидоров", "+7-495-111-22-33", 3, "BLOCKED", "50", true, false, "vip", "ночь");
        OwnerTestData.insert(jdbc, 4, "Анна Архивная", "+7-800-555-35-35", 2, "ACTIVE", "10", true, true);
        jdbc.update("update users set email = 'petr@mirea.ru' where id = 3");
    }

    @Test
    void defaultListHidesArchivedAndSortsNewestFirst() throws Exception {
        search("").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].name", contains("Пётр Сидоров", "Мария O'Нил_100%", "Иван Иванов")));
        search("archived=ARCHIVED").andExpect(jsonPath("$.content[*].name", contains("Анна Архивная")));
        search("archived=all").andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    void textSearchByNameEmailAndPhoneDigits() throws Exception {
        search("q=иванов").andExpect(jsonPath("$.content[*].name", contains("Иван Иванов")));
        search("q=MIREA.RU").andExpect(jsonPath("$.content[*].name", contains("Пётр Сидоров")));
        search("q=" + enc("912 345")).andExpect(jsonPath("$.content[*].name", contains("Мария O'Нил_100%")));
        search("q=" + enc("+7-495")).andExpect(jsonPath("$.content[*].name", contains("Пётр Сидоров")));
    }

    @Test
    void quotesAndWildcardsDoNotBreakQuery() throws Exception {
        search("q=" + enc("O'Нил_100%")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        search("q=" + enc("%")).andExpect(status().isOk()).andExpect(jsonPath("$.content[*].name", contains("Мария O'Нил_100%")));
        search("q=" + enc("_")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        search("q=" + enc("' or 1=1 --")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void filtersCombine() throws Exception {
        search("categoryId=2").andExpect(jsonPath("$.content[*].name", contains("Мария O'Нил_100%")));
        search("status=blocked").andExpect(jsonPath("$.content[*].name", contains("Пётр Сидоров")));
        search("notifications=false").andExpect(jsonPath("$.content[*].name", contains("Мария O'Нил_100%")));
        search("tag=VIP").andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Иван Иванов", "Пётр Сидоров")));
        search("tag=vip&status=ACTIVE").andExpect(jsonPath("$.content[*].name", contains("Иван Иванов")));
        search("loyalty=new&tag=ночь").andExpect(jsonPath("$.totalElements").value(2));
        search("loyalty=VIP").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void discountComparisonAndRegistrationPeriod() throws Exception {
        search("discount=15").andExpect(jsonPath("$.content[*].name", contains("Мария O'Нил_100%")));
        search("discount=15&discountOp=gte").andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Мария O'Нил_100%", "Пётр Сидоров")));
        search("discount=15&discountOp=lt").andExpect(jsonPath("$.content[*].name", contains("Иван Иванов")));
        search("discount=50&discountOp=lte&archived=ALL").andExpect(jsonPath("$.totalElements").value(4));
        search("discountOp=gt").andExpect(status().isBadRequest());

        search("from=2026-01-17T00:00&to=2026-01-18T23:59").andExpect(jsonPath("$.content[*].name", contains("Пётр Сидоров", "Мария O'Нил_100%")));
        search("from=2026-02-01T00:00&to=2026-01-01T00:00").andExpect(status().isBadRequest());
    }

    @Test
    void sortingAndPaging() throws Exception {
        search("sort=name&direction=asc").andExpect(jsonPath("$.content[*].name", contains("Иван Иванов", "Мария O'Нил_100%", "Пётр Сидоров")));
        search("sort=discount&direction=desc").andExpect(jsonPath("$.content[0].name").value("Пётр Сидоров"));
        search("sort=phone").andExpect(status().isBadRequest());
        search("direction=sideways").andExpect(status().isBadRequest());
        search("size=1000").andExpect(jsonPath("$.size").value(100));
        search("size=2&page=1&sort=name&direction=asc")
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[*].name", contains("Пётр Сидоров")));
    }

    @Test
    void unknownEnumValueIs400() throws Exception {
        search("loyalty=GOLD").andExpect(status().isBadRequest());
        search("archived=maybe").andExpect(status().isBadRequest());
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private ResultActions search(String query) throws Exception {
        return mvc.perform(get(URI.create("/api/owners?" + query)));
    }

}

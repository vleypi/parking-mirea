package ru.mirea.parking.owner;

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

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "OPERATOR")
class OwnerApiTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID ivan;

    @BeforeEach
    void seed() {
        OwnerTestData.clean(jdbc);
        jdbc.update("delete from audit_event");
        ivan = OwnerTestData.insert(jdbc, 1, "Иван Иванов", "+7-900-100-00-01", 1, "ACTIVE", "0", true, false, "vip");
        jdbc.update("insert into spot_types (id, code, title) values (1, 'STANDARD', 'Стандартное')");
        jdbc.update("insert into vehicles (id, uuid, user_id, license_plate, brand, model) values "
                + "(1, ?, 1, 'А123ВС777', 'Toyota', 'Camry'), (2, ?, 1, 'В456КМ177', 'Lada', 'Vesta')",
                UUID.randomUUID(), UUID.randomUUID());
    }

    @Test
    void getReturnsDtoWithCategoryAndVehicleCount() throws Exception {
        mvc.perform(get("/api/owners/" + ivan))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ivan.toString()))
                .andExpect(jsonPath("$.name").value("Иван Иванов"))
                .andExpect(jsonPath("$.category.title").value("Обычный"))
                .andExpect(jsonPath("$.status.code").value("ACTIVE"))
                .andExpect(jsonPath("$.vehiclesCount").value(2))
                .andExpect(jsonPath("$.tags[0]").value("vip"))
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.uuid").doesNotExist());
    }

    @Test
    void createWithDefaultsAndAudit() throws Exception {
        create("""
                {"name":"  Мария Петрова ","phone":"+7-900-100-00-02","email":"Maria@Example.com","categoryId":2,
                 "tags":[" Ночь ","ночь"]}""")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/owners/")))
                .andExpect(jsonPath("$.name").value("Мария Петрова"))
                .andExpect(jsonPath("$.email").value("maria@example.com"))
                .andExpect(jsonPath("$.category.code").value("RESIDENT"))
                .andExpect(jsonPath("$.status.code").value("ACTIVE"))
                .andExpect(jsonPath("$.discountPercent").value(0))
                .andExpect(jsonPath("$.notificationsEnabled").value(true))
                .andExpect(jsonPath("$.vehiclesCount").value(0))
                .andExpect(jsonPath("$.tags.length()").value(1));

        mvc.perform(get("/api/owners/" + ivan));
        Integer audit = jdbc.queryForObject(
                "select count(*) from audit_event where event_type = 'CREATE' and entity_type = 'OWNER'", Integer.class);
        org.assertj.core.api.Assertions.assertThat(audit).isEqualTo(1);
    }

    @Test
    void busyPhoneIsConflict() throws Exception {
        create("""
                {"name":"Двойник","phone":"+7-900-100-00-01","categoryId":1}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Телефон +7-900-100-00-01 уже указан у другого владельца"));
    }

    @Test
    void invalidFieldsAre400WithFieldNames() throws Exception {
        create("""
                {"name":"","phone":"89001000001","email":"not-an-email","categoryId":1,"discountPercent":75}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'phone')].message", hasItem("Телефон в формате +7-XXX-XXX-XX-XX")))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')].message", hasItem("Некорректный email")))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'discountPercent')].message", hasItem("Скидка от 0 до 50 %")))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'name')]").isNotEmpty());
    }

    @Test
    void unknownOrDisabledCategoryIs400() throws Exception {
        create("""
                {"name":"Пётр","phone":"+7-900-100-00-03","categoryId":99}""")
                .andExpect(status().isBadRequest());
        jdbc.update("update client_categories set active = false where id = 3");
        create("""
                {"name":"Пётр","phone":"+7-900-100-00-03","categoryId":3}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Категория «Сотрудник» отключена"));
    }

    @Test
    void updateChecksVersion() throws Exception {
        update(ivan, 0).andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value("BLOCKED"))
                .andExpect(jsonPath("$.discountPercent").value(10.5))
                .andExpect(jsonPath("$.version").value(1));

        update(ivan, 0).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Запись изменил другой пользователь, обновите карточку"));
    }

    @Test
    void unknownIdIs404AndMalformedIs400() throws Exception {
        mvc.perform(get("/api/owners/" + UUID.randomUUID())).andExpect(status().isNotFound());
        update(UUID.randomUUID(), 0).andExpect(status().isNotFound());
        mvc.perform(get("/api/owners/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerCanReadButNotChange() throws Exception {
        mvc.perform(get("/api/owners/" + ivan)).andExpect(status().isOk());
        create("""
                {"name":"Пётр","phone":"+7-900-100-00-03","categoryId":1}""").andExpect(status().isForbidden());
        update(ivan, 0).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void categoriesListIsReadable() throws Exception {
        mvc.perform(get("/api/references/client-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem("Резидент")))
                .andExpect(jsonPath("$[0].code").value("REGULAR"));
    }

    private ResultActions create(String json) throws Exception {
        return mvc.perform(post("/api/owners").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions update(UUID id, long version) throws Exception {
        return mvc.perform(put("/api/owners/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Иван Иванов","phone":"+7-900-100-00-01","email":null,"categoryId":2,"status":"BLOCKED",
                 "discountPercent":10.5,"notificationsEnabled":false,"tags":["vip"],"version":%d}""".formatted(version)));
    }
}

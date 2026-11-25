package ru.mirea.parking.error;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Все коды ошибок отдают одинаковый JSON без stack trace. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ErrorHandlingTest.ThrowingController.class)
class ErrorHandlingTest {

    @RestController
    @RequestMapping("/api/test-errors")
    static class ThrowingController {

        @GetMapping("/conflict")
        void conflict() {
            throw new ConflictException("Номер места 5 уже занят");
        }

        @GetMapping("/bad-field")
        void badField() {
            throw new BadRequestException("spotTypeId", "Тип места «Для электромобилей» отключён");
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw new ForbiddenException("Окончательное удаление отключено");
        }

        @GetMapping("/stale")
        void stale() {
            throw new ObjectOptimisticLockingFailureException("Spot", 1L);
        }

        @GetMapping("/integrity")
        void integrity() {
            throw new DataIntegrityViolationException("duplicate key value violates unique constraint \"spots_spot_number_key\"");
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("секретная деталь: select * from user_account");
        }
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void unauthenticatedIs401InCommonFormat() throws Exception {
        expectCommonFormat(mvc.perform(get("/api/owners")), 401, "UNAUTHORIZED");
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewerChangingDataIs403InCommonFormat() throws Exception {
        expectCommonFormat(mvc.perform(post("/api/owners").contentType(MediaType.APPLICATION_JSON).content("{}")),
                403, "FORBIDDEN");
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void beanValidationIs400WithFieldErrors() throws Exception {
        expectCommonFormat(mvc.perform(post("/api/owners").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"phone\":\"123\",\"categoryId\":1}")), 400, "VALIDATION_FAILED")
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("phone")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void unknownSortFieldAndBadUuidAre400() throws Exception {
        expectCommonFormat(mvc.perform(get("/api/owners").param("sort", "password")), 400, "BAD_REQUEST")
                .andExpect(jsonPath("$.message", containsString("password")));
        expectCommonFormat(mvc.perform(get("/api/owners/not-a-uuid")), 400, "BAD_REQUEST")
                .andExpect(jsonPath("$.fieldErrors[0].field").value("id"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void unknownEnumInFilterIs400() throws Exception {
        expectCommonFormat(mvc.perform(get("/api/owners").param("status", "SLEEPING")), 400, "VALIDATION_FAILED")
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"));
    }

    @Test
    @WithMockUser(roles = "OPERATOR")
    void unknownJsonFieldIs400() throws Exception {
        expectCommonFormat(mvc.perform(post("/api/owners").contentType(MediaType.APPLICATION_JSON)
                .content("{\"hacker\":true}")), 400, "BAD_REQUEST")
                .andExpect(jsonPath("$.fieldErrors[0].field").value("hacker"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void missingRecordIs404() throws Exception {
        expectCommonFormat(mvc.perform(get("/api/owners/" + UUID.randomUUID())), 404, "NOT_FOUND");
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void applicationExceptionsKeepTheirStatusAndMessage() throws Exception {
        expectCommonFormat(mvc.perform(get("/api/test-errors/conflict")), 409, "CONFLICT")
                .andExpect(jsonPath("$.message").value("Номер места 5 уже занят"));
        expectCommonFormat(mvc.perform(get("/api/test-errors/bad-field")), 400, "BAD_REQUEST")
                .andExpect(jsonPath("$.fieldErrors[0].field").value("spotTypeId"));
        expectCommonFormat(mvc.perform(get("/api/test-errors/forbidden")), 403, "FORBIDDEN");
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void staleVersionAndUniqueViolationAre409WithoutSqlDetails() throws Exception {
        expectCommonFormat(mvc.perform(get("/api/test-errors/stale")), 409, "STALE_VERSION")
                .andExpect(jsonPath("$.message").value(GlobalExceptionHandler.STALE_VERSION_MESSAGE));
        expectCommonFormat(mvc.perform(get("/api/test-errors/integrity")), 409, "DATA_CONFLICT")
                .andExpect(content().string(not(containsString("spots_spot_number_key"))));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void unexpectedErrorIs500WithoutStackTrace() throws Exception {
        expectCommonFormat(mvc.perform(get("/api/test-errors/boom")), 500, "INTERNAL_ERROR")
                .andExpect(content().string(not(containsString("секретная деталь"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("at ru.mirea"))));
    }

    private static ResultActions expectCommonFormat(ResultActions result, int status, String error) throws Exception {
        return result.andExpect(status().is(status))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }
}

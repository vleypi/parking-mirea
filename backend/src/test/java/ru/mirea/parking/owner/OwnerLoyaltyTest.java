package ru.mirea.parking.owner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.parking.domain.LoyaltyLevel;
import ru.mirea.parking.domain.Owner;
import ru.mirea.parking.filter.OwnerSpecifications;
import ru.mirea.parking.repository.OwnerRepository;
import ru.mirea.parking.service.LoyaltyCalculator;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Владельцы на границах правила: 0, 2, 3, 9, 10 завершённых заявок.
 * Заявки в других статусах не считаются, архивные завершённые — считаются.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(roles = "VIEWER")
class OwnerLoyaltyTest {

    private static final Map<Long, Integer> COMPLETED_BY_OWNER = Map.of(1L, 0, 2L, 2, 3L, 3, 4L, 9, 5L, 10);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private OwnerRepository owners;

    private final Map<Long, UUID> uuids = new java.util.HashMap<>();

    @BeforeEach
    void seed() {
        OwnerTestData.clean(jdbc);
        jdbc.update("insert into request_statuses (id, code, title) values (1, 'NEW', 'Новая'), (2, 'CONFIRMED', 'Подтверждена'), "
                + "(3, 'COMPLETED', 'Завершена'), (4, 'CANCELLED', 'Отменена')");
        jdbc.update("insert into spot_types (id, code, title) values (1, 'STANDARD', 'Стандартное')");
        jdbc.update("insert into spots (id, uuid, spot_number, spot_type_id, hourly_rate) values (1, ?, 1, 1, 100.00)", UUID.randomUUID());

        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 9, 0);
        int requestId = 1;
        for (var entry : COMPLETED_BY_OWNER.entrySet()) {
            long owner = entry.getKey();
            uuids.put(owner, OwnerTestData.insert(jdbc, owner, "Владелец " + owner, "+7-900-100-00-1" + owner, 1, "ACTIVE", "0", true, false));
            jdbc.update("insert into vehicles (id, uuid, user_id, license_plate, brand, model) values (?, ?, ?, ?, 'Lada', 'Vesta')",
                    owner, UUID.randomUUID(), owner, "А00" + owner + "ВС77");
            for (int i = 0; i < entry.getValue(); i++) {
                boolean archived = i == 0;
                insertRequest(requestId++, owner, 3, start.plusDays(requestId), archived);
            }
            // Отменённая и новая заявки на уровень не влияют.
            insertRequest(requestId++, owner, 4, start.plusDays(requestId), false);
            insertRequest(requestId++, owner, 1, start.plusDays(requestId), false);
        }
    }

    @Test
    void responseShowsLoyaltyAndCompletedCount() throws Exception {
        mvc.perform(get("/api/owners/" + uuids.get(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedRequests").value(2))
                .andExpect(jsonPath("$.loyalty.code").value("NEW"))
                .andExpect(jsonPath("$.loyalty.title").value("Новый"));
        mvc.perform(get("/api/owners/" + uuids.get(3L))).andExpect(jsonPath("$.loyalty.code").value("REGULAR"));
        mvc.perform(get("/api/owners/" + uuids.get(5L))).andExpect(jsonPath("$.loyalty.code").value("VIP"));
    }

    @Test
    void levelChangesAfterCompletingRequest() throws Exception {
        mvc.perform(get("/api/owners/" + uuids.get(4L))).andExpect(jsonPath("$.loyalty.code").value("REGULAR"));
        jdbc.update("update requests set status_id = 3 where user_id = 4 and status_id = 1");
        mvc.perform(get("/api/owners/" + uuids.get(4L)))
                .andExpect(jsonPath("$.completedRequests").value(10))
                .andExpect(jsonPath("$.loyalty.code").value("VIP"));
    }

    @ParameterizedTest
    @EnumSource(LoyaltyLevel.class)
    @Transactional
    void specificationSelectsSameOwnersAsCalculator(LoyaltyLevel level) {
        var expected = COMPLETED_BY_OWNER.entrySet().stream()
                .filter(e -> LoyaltyCalculator.calculate(e.getValue()) == level)
                .map(e -> uuids.get(e.getKey()))
                .collect(Collectors.toSet());

        var actual = owners.findAll(OwnerSpecifications.hasLoyalty(level)).stream()
                .map(Owner::getUuid)
                .collect(Collectors.toSet());

        assertThat(actual).isNotEmpty().isEqualTo(expected);
    }

    private void insertRequest(int id, long owner, int status, LocalDateTime start, boolean archived) {
        jdbc.update("insert into requests (id, uuid, code, user_id, vehicle_id, spot_id, status_id, start_time, end_time, "
                        + "cost, paid, archived, created_at, updated_at, version) values (?, ?, ?, ?, ?, 1, ?, ?, ?, 100, false, ?, now(), now(), 0)",
                id, UUID.randomUUID(), "REQ-%06d".formatted(id), owner, owner, status, start, start.plusHours(1), archived);
    }
}

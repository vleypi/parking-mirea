package ru.mirea.parking.request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.parking.domain.Request;
import ru.mirea.parking.domain.RequestPriority;
import ru.mirea.parking.repository.RequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.mirea.parking.filter.RequestSpecifications.hasPriority;
import static ru.mirea.parking.service.RequestPriorityCalculator.calculate;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RequestPrioritySpecificationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 1, 12, 0);
    private static final List<String> STATUSES = List.of("NEW", "CONFIRMED", "COMPLETED", "CANCELLED");
    private static final List<Long> OFFSETS_MINUTES = List.of(-60L, 0L, 1439L, 1440L, 1441L, 4319L, 4320L, 14400L);

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private RequestRepository requests;

    @BeforeEach
    void seed() {
        for (String table : List.of("request_tag", "requests", "vehicles", "spots", "users", "request_statuses")) {
            jdbc.update("delete from " + table);
        }
        jdbc.update("insert into request_statuses (id, code, title) values "
                + "(1, 'NEW', 'Новая'), (2, 'CONFIRMED', 'Подтверждена'), (3, 'COMPLETED', 'Завершена'), (4, 'CANCELLED', 'Отменена')");
        jdbc.update("insert into users (id, uuid, name, phone, created_at) values (1, ?, 'Иван', '+7-900-100-00-01', now())",
                UUID.randomUUID());
        jdbc.update("insert into vehicles (id, uuid, user_id, license_plate, brand, model) values (1, ?, 1, 'А123ВС777', 'Toyota', 'Camry')",
                UUID.randomUUID());
        jdbc.update("insert into spots (id, uuid, spot_number, hourly_rate) values (1, ?, 1, 100.00)", UUID.randomUUID());

        for (int s = 0; s < STATUSES.size(); s++) {
            for (long offset : OFFSETS_MINUTES) {
                LocalDateTime start = NOW.plusMinutes(offset);
                jdbc.update("insert into requests (uuid, user_id, vehicle_id, spot_id, status_id, start_time, end_time, "
                                + "cost, paid, archived, created_at, updated_at, version) "
                                + "values (?, 1, 1, 1, ?, ?, ?, 0, false, false, now(), now(), 0)",
                        UUID.randomUUID(), s + 1, start, start.plusHours(1));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(RequestPriority.class)
    void specificationSelectsSameRequestsAsCalculator(RequestPriority priority) {
        List<Request> all = requests.findAll();
        assertThat(all).hasSize(STATUSES.size() * OFFSETS_MINUTES.size());

        List<Long> expected = all.stream()
                .filter(r -> calculate(r.getStatus().getCode(), r.getStartTime(), NOW) == priority)
                .map(Request::getId)
                .toList();
        List<Long> actual = requests.findAll(hasPriority(priority, NOW)).stream()
                .map(Request::getId)
                .toList();

        assertThat(expected).isNotEmpty();
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }
}

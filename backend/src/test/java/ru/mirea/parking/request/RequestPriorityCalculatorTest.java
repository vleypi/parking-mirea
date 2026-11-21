package ru.mirea.parking.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.mirea.parking.domain.RequestPriority;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.mirea.parking.service.RequestPriorityCalculator.calculate;

class RequestPriorityCalculatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 12, 1, 12, 0);

    @ParameterizedTest(name = "{0}, начало через {1} мин -> {2}")
    @CsvSource({
            "NEW,       -60,   HIGH",
            "NEW,       0,     HIGH",
            "NEW,       1439,  HIGH",
            "NEW,       1440,  MEDIUM",
            "NEW,       4319,  MEDIUM",
            "NEW,       4320,  LOW",
            "CONFIRMED, -60,   MEDIUM",
            "CONFIRMED, 60,    MEDIUM",
            "CONFIRMED, 4319,  MEDIUM",
            "CONFIRMED, 4320,  LOW",
            "COMPLETED, 60,    LOW",
            "CANCELLED, 60,    LOW",
            "CANCELLED, 10000, LOW"
    })
    void calculatesPriority(String status, long minutesUntilStart, RequestPriority expected) {
        LocalDateTime start = NOW.plusMinutes(minutesUntilStart);
        assertThat(calculate(status, start, NOW)).isEqualTo(expected);
    }

    @Test
    void priorityChangesWhenStartTimeMoves() {
        LocalDateTime start = NOW.plusDays(5);
        assertThat(calculate("NEW", start, NOW)).isEqualTo(RequestPriority.LOW);
        assertThat(calculate("NEW", start, start.minus(Duration.ofDays(2)))).isEqualTo(RequestPriority.MEDIUM);
        assertThat(calculate("NEW", start, start.minusHours(3))).isEqualTo(RequestPriority.HIGH);
    }
}

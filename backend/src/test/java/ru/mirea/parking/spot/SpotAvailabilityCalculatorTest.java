package ru.mirea.parking.spot;

import org.junit.jupiter.api.Test;
import ru.mirea.parking.domain.SpotAvailability;
import ru.mirea.parking.domain.SpotStatus;
import ru.mirea.parking.service.SpotAvailabilityCalculator;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SpotAvailabilityCalculatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 12, 0);

    @Test
    void maintenanceAndClosedAreAlwaysUnavailable() {
        assertThat(SpotAvailabilityCalculator.calculate(SpotStatus.MAINTENANCE, true)).isEqualTo(SpotAvailability.UNAVAILABLE);
        assertThat(SpotAvailabilityCalculator.calculate(SpotStatus.MAINTENANCE, false)).isEqualTo(SpotAvailability.UNAVAILABLE);
        assertThat(SpotAvailabilityCalculator.calculate(SpotStatus.CLOSED, false)).isEqualTo(SpotAvailability.UNAVAILABLE);
    }

    @Test
    void workingSpotIsBusyOrFree() {
        assertThat(SpotAvailabilityCalculator.calculate(SpotStatus.AVAILABLE, true)).isEqualTo(SpotAvailability.BUSY_NOW);
        assertThat(SpotAvailabilityCalculator.calculate(SpotStatus.AVAILABLE, false)).isEqualTo(SpotAvailability.FREE_NOW);
    }

    @Test
    void periodStartIsIncludedEndIsExcluded() {
        assertThat(SpotAvailabilityCalculator.covers(NOW, NOW.plusHours(2), NOW)).isTrue();
        assertThat(SpotAvailabilityCalculator.covers(NOW.minusHours(2), NOW, NOW)).isFalse();
        assertThat(SpotAvailabilityCalculator.covers(NOW.minusHours(1), NOW.plusMinutes(1), NOW)).isTrue();
        assertThat(SpotAvailabilityCalculator.covers(NOW.plusMinutes(1), NOW.plusHours(1), NOW)).isFalse();
    }
}

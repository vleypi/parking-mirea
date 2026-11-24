package ru.mirea.parking.vehicle;

import org.junit.jupiter.api.Test;
import ru.mirea.parking.domain.VehicleActivity;
import ru.mirea.parking.service.VehicleActivityCalculator;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VehicleActivityCalculatorTest {

    @Test
    void boundariesOfLevels() {
        assertThat(VehicleActivityCalculator.calculate(0)).isEqualTo(VehicleActivity.LOW);
        assertThat(VehicleActivityCalculator.calculate(1)).isEqualTo(VehicleActivity.MEDIUM);
        assertThat(VehicleActivityCalculator.calculate(3)).isEqualTo(VehicleActivity.MEDIUM);
        assertThat(VehicleActivityCalculator.calculate(4)).isEqualTo(VehicleActivity.HIGH);
        assertThat(VehicleActivityCalculator.calculate(50)).isEqualTo(VehicleActivity.HIGH);
    }

    @Test
    void negativeCountIsRejected() {
        assertThatThrownBy(() -> VehicleActivityCalculator.calculate(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void windowIsThirtyDays() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 31, 12, 0);
        assertThat(VehicleActivityCalculator.windowStart(now)).isEqualTo(LocalDateTime.of(2026, 3, 1, 12, 0));
    }
}

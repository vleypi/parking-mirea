package ru.mirea.parking.owner;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import ru.mirea.parking.domain.LoyaltyLevel;
import ru.mirea.parking.service.LoyaltyCalculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoyaltyCalculatorTest {

    @ParameterizedTest(name = "{0} завершённых заявок -> {1}")
    @CsvSource({
            "0, NEW",
            "2, NEW",       // граница 2/3: ещё новый
            "3, REGULAR",   // граница 2/3: уже постоянный
            "9, REGULAR",   // граница 9/10: ещё постоянный
            "10, VIP",      // граница 9/10: уже VIP
            "250, VIP"
    })
    void levelByCompletedRequests(long completed, LoyaltyLevel expected) {
        assertThat(LoyaltyCalculator.calculate(completed)).isEqualTo(expected);
    }

    @Test
    void negativeCountIsRejected() {
        assertThatThrownBy(() -> LoyaltyCalculator.calculate(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}

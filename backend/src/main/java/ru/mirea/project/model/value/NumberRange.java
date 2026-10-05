package ru.mirea.project.model.value;

import java.math.BigDecimal;

import ru.mirea.project.exception.BusinessException;

public record NumberRange(BigDecimal min, BigDecimal max) {
    public NumberRange {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BusinessException("Нижняя граница не может быть больше верхней");
        }
    }

    public boolean isUnbounded() {
        return min == null && max == null;
    }

    public boolean contains(BigDecimal value) {
        return (min == null || value.compareTo(min) >= 0) && (max == null || value.compareTo(max) <= 0);
    }

    public boolean contains(long value) {
        return contains(BigDecimal.valueOf(value));
    }

    @Override
    public String toString() {
        if (min == null) {
            return "до " + format(max);
        }
        if (max == null) {
            return "от " + format(min);
        }
        return "от " + format(min) + " до " + format(max);
    }

    private static String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}

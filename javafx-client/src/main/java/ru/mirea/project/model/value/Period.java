package ru.mirea.project.model.value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import ru.mirea.project.exception.BusinessException;

public record Period(LocalDateTime from, LocalDateTime to) {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public Period {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("Начало периода не может быть позже конца");
        }
    }

    public static Period allTime() {
        return new Period(null, null);
    }

    public boolean isAllTime() {
        return from == null && to == null;
    }

    public boolean isBounded() {
        return from != null && to != null;
    }

    public boolean contains(LocalDateTime moment) {
        return (from == null || !moment.isBefore(from)) && (to == null || !moment.isAfter(to));
    }

    public boolean overlaps(LocalDateTime start, LocalDateTime end) {
        return (from == null || end.isAfter(from)) && (to == null || start.isBefore(to));
    }

    @Override
    public String toString() {
        if (isAllTime()) {
            return "за всё время";
        }
        if (from == null) {
            return "по " + FORMAT.format(to);
        }
        if (to == null) {
            return "с " + FORMAT.format(from);
        }
        return "с " + FORMAT.format(from) + " по " + FORMAT.format(to);
    }
}

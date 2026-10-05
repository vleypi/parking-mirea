package ru.mirea.project.model;

import java.util.Arrays;
import java.util.Optional;

public enum RequestStatus implements LookupValue {
    NEW(1, "Новая"),
    CONFIRMED(2, "Подтверждена"),
    COMPLETED(3, "Завершена"),
    CANCELLED(4, "Отменена");

    private final int id;
    private final String title;

    RequestStatus(int id, String title) {
        this.id = id;
        this.title = title;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public static Optional<RequestStatus> fromId(int id) {
        return Arrays.stream(values())
            .filter(status -> status.id == id)
            .findFirst();
    }

    @Override
    public String toString() {
        return title;
    }
}

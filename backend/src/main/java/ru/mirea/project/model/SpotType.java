package ru.mirea.project.model;

import java.util.Arrays;
import java.util.Optional;

public enum SpotType implements LookupValue {
    STANDARD(1, "Стандартное"),
    DISABLED(2, "Для людей с инвалидностью"),
    ELECTRIC(3, "Для электромобилей");

    private final int id;
    private final String title;

    SpotType(int id, String title) {
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

    public static Optional<SpotType> fromId(int id) {
        return Arrays.stream(values())
            .filter(type -> type.id == id)
            .findFirst();
    }

    @Override
    public String toString() {
        return title;
    }
}

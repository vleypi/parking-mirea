package ru.mirea.project.util;

import java.nio.file.Path;
import java.util.List;

import ru.mirea.project.model.ParkingRequest;
import ru.mirea.project.model.User;

public final class ExcelExporter {

    private ExcelExporter() {
    }

    public static void export(List<User> users, List<ParkingRequest> requests, Path file) {
        throw new UnsupportedOperationException("Не реализовано");
    }
}

package ru.mirea.project.fx;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.DataAccessException;
import ru.mirea.project.exception.EntityNotFoundException;

public final class Alerts {
    private Alerts() {
    }

    public static void info(String title, String message) {
        show(AlertType.INFORMATION, title, message);
    }

    public static void error(String message) {
        show(AlertType.ERROR, "Ошибка", message);
    }

    public static void error(RuntimeException exception) {
        if (exception instanceof BusinessException || exception instanceof EntityNotFoundException
            || exception instanceof DataAccessException) {
            error(exception.getMessage());
        } else {
            error("Непредвиденная ошибка: " + exception.getMessage());
        }
    }

    public static boolean confirm(String message) {
        Alert alert = new Alert(AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle("Подтверждение");
        alert.setHeaderText(null);
        alert.getDialogPane().getStylesheets().add(Views.stylesheet());
        return alert.showAndWait().filter(ButtonType.YES::equals).isPresent();
    }

    private static void show(AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.getDialogPane().getStylesheets().add(Views.stylesheet());
        alert.showAndWait();
    }
}

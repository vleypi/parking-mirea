package ru.mirea.project.fx;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;

import ru.mirea.project.AppContext;

public class MainWindowController {
    private final AppContext context;

    public MainWindowController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void showGeneralStatistics() {
        try {
            FXMLLoader loader = Views.load("GeneralStatisticsDialog.fxml", context);
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Общая статистика");
            dialog.setDialogPane(loader.getRoot());
            dialog.getDialogPane().getStylesheets().add(Views.stylesheet());
            dialog.showAndWait();
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }

    @FXML
    private void showAbout() {
        Alerts.info("О программе", "Парковочная система. JavaFX-клиент к базе данных PostgreSQL из КР1.");
    }
}

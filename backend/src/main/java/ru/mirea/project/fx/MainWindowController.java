package ru.mirea.project.fx;

import javafx.fxml.FXML;

import ru.mirea.project.AppContext;

public class MainWindowController {
    private final AppContext context;

    public MainWindowController(AppContext context) {
        this.context = context;
    }

    @FXML
    private void showGeneralStatistics() {
        Alerts.info("Общая статистика", "Окно общей статистики ещё не готово.");
    }

    @FXML
    private void showAbout() {
        Alerts.info("О программе", "Парковочная система. JavaFX-клиент к базе данных PostgreSQL из КР1.");
    }
}

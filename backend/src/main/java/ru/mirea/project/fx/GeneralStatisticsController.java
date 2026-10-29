package ru.mirea.project.fx;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import ru.mirea.project.AppContext;
import ru.mirea.project.dto.statistics.GeneralStatistics;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.service.StatisticsService;

/**
 * Окно «Общая статистика» (меню «Отчёты» главного окна).
 * 9 показателей из StatisticsService.getGeneralStatistics — те же, что в консольной версии.
 */
public class GeneralStatisticsController {
    private final StatisticsService statisticsService;

    @FXML
    private DatePicker fromPicker;
    @FXML
    private DatePicker toPicker;
    @FXML
    private Label errorLabel;
    @FXML
    private Label periodValue;
    @FXML
    private Label usersValue;
    @FXML
    private Label vehiclesValue;
    @FXML
    private Label spotsValue;
    @FXML
    private Label requestsValue;
    @FXML
    private Label activeValue;
    @FXML
    private Label completedValue;
    @FXML
    private Label cancelledValue;
    @FXML
    private Label occupiedValue;
    @FXML
    private Label revenueValue;

    public GeneralStatisticsController(AppContext context) {
        this.statisticsService = context.getStatisticsService();
    }

    @FXML
    private void initialize() {
        calculate();
    }

    @FXML
    private void calculate() {
        errorLabel.setText("");
        try {
            GeneralStatistics statistics = statisticsService.getGeneralStatistics(
                UserController.readPeriod(fromPicker, toPicker));
            periodValue.setText(statistics.period().toString());
            usersValue.setText(String.valueOf(statistics.totalUsers()));
            vehiclesValue.setText(String.valueOf(statistics.totalVehicles()));
            spotsValue.setText(String.valueOf(statistics.totalSpots()));
            requestsValue.setText(String.valueOf(statistics.requests()));
            activeValue.setText(String.valueOf(statistics.active()));
            completedValue.setText(String.valueOf(statistics.completed()));
            cancelledValue.setText(String.valueOf(statistics.cancelled()));
            occupiedValue.setText(String.valueOf(statistics.occupiedNow()));
            revenueValue.setText(statistics.revenue().toPlainString());
        } catch (BusinessException e) {
            errorLabel.setText(e.getMessage());
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }
}

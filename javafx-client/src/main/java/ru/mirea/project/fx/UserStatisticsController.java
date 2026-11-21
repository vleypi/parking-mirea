package ru.mirea.project.fx;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import ru.mirea.project.AppContext;
import ru.mirea.project.dto.statistics.UserStatistics;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.enums.RequestStatus;
import ru.mirea.project.service.UserService;

/**
 * Окно статистики выбранного владельца (UserStatisticsDialog.fxml).
 * Показатели считает UserService.getStatistics — те же, что в консольной версии.
 */
public class UserStatisticsController {
    private final UserService userService;
    private User user;

    @FXML
    private DatePicker fromPicker;
    @FXML
    private DatePicker toPicker;
    @FXML
    private Label userLabel;
    @FXML
    private Label errorLabel;
    @FXML
    private Label periodValue;
    @FXML
    private Label vehiclesValue;
    @FXML
    private Label newValue;
    @FXML
    private Label confirmedValue;
    @FXML
    private Label completedValue;
    @FXML
    private Label cancelledValue;
    @FXML
    private Label paidValue;

    public UserStatisticsController(AppContext context) {
        this.userService = context.getUserService();
    }

    public void prepare(User user) {
        this.user = user;
        userLabel.setText(user.getName() + " — " + user.getPhone());
        calculate();
    }

    @FXML
    private void calculate() {
        errorLabel.setText("");
        try {
            // Пустые DatePicker дают Period(null, null) — это «за всё время»
            UserStatistics statistics = userService.getStatistics(user.getId(),
                UserController.readPeriod(fromPicker, toPicker));
            periodValue.setText(statistics.period().toString());
            vehiclesValue.setText(String.valueOf(statistics.vehicleCount()));
            newValue.setText(String.valueOf(count(statistics, RequestStatus.NEW)));
            confirmedValue.setText(String.valueOf(count(statistics, RequestStatus.CONFIRMED)));
            completedValue.setText(String.valueOf(count(statistics, RequestStatus.COMPLETED)));
            cancelledValue.setText(String.valueOf(count(statistics, RequestStatus.CANCELLED)));
            paidValue.setText(statistics.totalPaid().toPlainString());
        } catch (BusinessException e) {
            errorLabel.setText(e.getMessage());
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }

    private long count(UserStatistics statistics, RequestStatus status) {
        return statistics.requestsByStatus().getOrDefault(status, 0L);
    }
}

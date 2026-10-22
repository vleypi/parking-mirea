package ru.mirea.project.fx;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import ru.mirea.project.AppContext;
import ru.mirea.project.dto.statistics.RequestStatistics;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.enums.RequestStatus;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.service.RequestService;

public class RequestStatisticsController {
    private final RequestService requestService;

    @FXML
    private DatePicker fromPicker;
    @FXML
    private DatePicker toPicker;
    @FXML
    private Label errorLabel;
    @FXML
    private Label periodValue;
    @FXML
    private Label totalValue;
    @FXML
    private Label newValue;
    @FXML
    private Label confirmedValue;
    @FXML
    private Label completedValue;
    @FXML
    private Label cancelledValue;
    @FXML
    private Label revenueValue;
    @FXML
    private Label averageValue;

    public RequestStatisticsController(AppContext context) {
        this.requestService = context.getRequestService();
    }

    @FXML
    private void initialize() {
        errorLabel.managedProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());
        calculate();
    }

    @FXML
    private void calculate() {
        errorLabel.setText("");
        try {
            RequestStatistics statistics = requestService.getStatistics(readPeriod());
            long total = statistics.requestsByStatus().values().stream().mapToLong(Long::longValue).sum();

            periodValue.setText(statistics.period().toString());
            totalValue.setText(String.valueOf(total));
            newValue.setText(String.valueOf(count(statistics, RequestStatus.NEW)));
            confirmedValue.setText(String.valueOf(count(statistics, RequestStatus.CONFIRMED)));
            completedValue.setText(String.valueOf(count(statistics, RequestStatus.COMPLETED)));
            cancelledValue.setText(String.valueOf(count(statistics, RequestStatus.CANCELLED)));
            revenueValue.setText(statistics.revenue().toPlainString());
            averageValue.setText(statistics.averageDurationHours().toPlainString());
        } catch (BusinessException e) {
            errorLabel.setText(e.getMessage());
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }

    private long count(RequestStatistics statistics, RequestStatus status) {
        return statistics.requestsByStatus().getOrDefault(status, 0L);
    }

    private Period readPeriod() {
        LocalDate fromDate = fromPicker.getValue();
        LocalDate toDate = toPicker.getValue();
        LocalDateTime from = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? null : toDate.atTime(LocalTime.MAX);
        return new Period(from, to);
    }
}

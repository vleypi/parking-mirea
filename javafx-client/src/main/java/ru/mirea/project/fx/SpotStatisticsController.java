package ru.mirea.project.fx;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import ru.mirea.project.AppContext;
import ru.mirea.project.dto.statistics.SpotStatistics;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.service.SpotService;

public class SpotStatisticsController {
    private final SpotService spotService;

    private Spot spot;

    @FXML
    private DatePicker fromPicker;
    @FXML
    private DatePicker toPicker;
    @FXML
    private Label spotLabel;
    @FXML
    private Label errorLabel;
    @FXML
    private Label periodValue;
    @FXML
    private Label requestCountValue;
    @FXML
    private Label occupiedHoursValue;
    @FXML
    private Label revenueValue;

    public SpotStatisticsController(AppContext context) {
        this.spotService = context.getSpotService();
    }

    @FXML
    private void initialize() {
        errorLabel.managedProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());
    }

    public void prepare(Spot selectedSpot) {
        this.spot = selectedSpot;
        spotLabel.setText("Место №" + spot.getSpotNumber() + " (" + spot.getSpotType().getTitle() + ")");
        calculate();
    }

    @FXML
    private void calculate() {
        errorLabel.setText("");
        try {
            SpotStatistics statistics = spotService.getStatistics(spot.getId(), readPeriod());
            periodValue.setText(statistics.period().toString());
            requestCountValue.setText(String.valueOf(statistics.requestCount()));
            occupiedHoursValue.setText(statistics.occupiedHours().toPlainString());
            revenueValue.setText(statistics.revenue().toPlainString());
        } catch (BusinessException | EntityNotFoundException e) {
            errorLabel.setText(e.getMessage());
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }

    private Period readPeriod() {
        LocalDate fromDate = fromPicker.getValue();
        LocalDate toDate = toPicker.getValue();
        LocalDateTime from = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? null : toDate.atTime(LocalTime.MAX);
        return new Period(from, to);
    }
}

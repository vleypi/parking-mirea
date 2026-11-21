package ru.mirea.project.fx;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import ru.mirea.project.AppContext;
import ru.mirea.project.dto.statistics.VehicleStatistics;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.model.enums.RequestStatus;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.service.VehicleService;

public class VehicleStatisticsController {
    private final VehicleService vehicleService;
    private Vehicle vehicle;

    @FXML
    private DatePicker fromPicker;
    @FXML
    private DatePicker toPicker;
    @FXML
    private Label vehicleLabel;
    @FXML
    private Label errorLabel;
    @FXML
    private Label periodValue;
    @FXML
    private Label newValue;
    @FXML
    private Label confirmedValue;
    @FXML
    private Label completedValue;
    @FXML
    private Label cancelledValue;
    @FXML
    private Label hoursValue;
    @FXML
    private Label paidValue;

    public VehicleStatisticsController(AppContext context) {
        this.vehicleService = context.getVehicleService();
    }

    public void prepare(Vehicle vehicle) {
        this.vehicle = vehicle;
        vehicleLabel.setText(vehicle.getLicensePlate() + " — " + vehicle.getBrand() + " " + vehicle.getModel());
        calculate();
    }

    @FXML
    private void calculate() {
        errorLabel.setText("");
        try {
            VehicleStatistics statistics = vehicleService.getStatistics(vehicle.getId(), readPeriod());
            periodValue.setText(statistics.period().toString());
            newValue.setText(String.valueOf(count(statistics, RequestStatus.NEW)));
            confirmedValue.setText(String.valueOf(count(statistics, RequestStatus.CONFIRMED)));
            completedValue.setText(String.valueOf(count(statistics, RequestStatus.COMPLETED)));
            cancelledValue.setText(String.valueOf(count(statistics, RequestStatus.CANCELLED)));
            hoursValue.setText(statistics.parkedHours().toPlainString());
            paidValue.setText(statistics.totalPaid().toPlainString());
        } catch (BusinessException e) {
            errorLabel.setText(e.getMessage());
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }

    private long count(VehicleStatistics statistics, RequestStatus status) {
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
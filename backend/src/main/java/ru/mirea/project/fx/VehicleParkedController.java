package ru.mirea.project.fx;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import ru.mirea.project.AppContext;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;

public class VehicleParkedController {
    private final VehicleService vehicleService;
    private final UserService userService;

    private final ObservableList<Vehicle> result = FXCollections.observableArrayList();
    private Map<Long, String> ownerNames = Map.of();

    @FXML
    private DatePicker fromPicker;
    @FXML
    private DatePicker toPicker;
    @FXML
    private Label errorLabel;
    @FXML
    private Label placeholderLabel;
    @FXML
    private TableView<Vehicle> resultTable;
    @FXML
    private TableColumn<Vehicle, String> plateColumn;
    @FXML
    private TableColumn<Vehicle, String> brandColumn;
    @FXML
    private TableColumn<Vehicle, String> modelColumn;
    @FXML
    private TableColumn<Vehicle, String> ownerColumn;

    public VehicleParkedController(AppContext context) {
        this.vehicleService = context.getVehicleService();
        this.userService = context.getUserService();
    }

    @FXML
    private void initialize() {
        plateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLicensePlate()));
        brandColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBrand()));
        modelColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getModel()));
        ownerColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(ownerNames.getOrDefault(cell.getValue().getUserId(), "—")));
        resultTable.setItems(result);
    }

    @FXML
    private void search() {
        errorLabel.setText("");
        result.clear();
        try {
            Period period = readPeriod();
            ownerNames = userService.getAll().stream()
                .collect(Collectors.toMap(User::getId, User::getName));
            result.setAll(vehicleService.findParkedDuring(period));
            placeholderLabel.setText("В этот период автомобили на парковке не стояли");
        } catch (BusinessException e) {
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
package ru.mirea.project.fx;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import ru.mirea.project.AppContext;
import ru.mirea.project.model.entity.Request;
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.model.enums.RequestStatus;
import ru.mirea.project.service.RequestService;
import ru.mirea.project.service.SpotService;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;

public class RequestController {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final RequestService requestService;
    private final UserService userService;
    private final VehicleService vehicleService;
    private final SpotService spotService;

    private final ObservableList<Request> requests = FXCollections.observableArrayList();
    private Map<Long, String> ownerNames = Map.of();
    private Map<Long, String> plates = Map.of();
    private Map<Long, Integer> spotNumbers = Map.of();

    @FXML
    private TableView<Request> requestTable;
    @FXML
    private TableColumn<Request, String> ownerColumn;
    @FXML
    private TableColumn<Request, String> vehicleColumn;
    @FXML
    private TableColumn<Request, Integer> spotColumn;
    @FXML
    private TableColumn<Request, LocalDateTime> startColumn;
    @FXML
    private TableColumn<Request, LocalDateTime> endColumn;
    @FXML
    private TableColumn<Request, RequestStatus> statusColumn;
    @FXML
    private Label countLabel;

    public RequestController(AppContext context) {
        this.requestService = context.getRequestService();
        this.userService = context.getUserService();
        this.vehicleService = context.getVehicleService();
        this.spotService = context.getSpotService();
    }

    @FXML
    private void initialize() {
        ownerColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(ownerNames.getOrDefault(cell.getValue().getUserId(), "?")));
        vehicleColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(plates.getOrDefault(cell.getValue().getVehicleId(), "?")));
        spotColumn.setCellValueFactory(cell ->
            new SimpleObjectProperty<>(spotNumbers.get(cell.getValue().getSpotId())));
        startColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getStartTime()));
        endColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getEndTime()));
        statusColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getStatus()));

        startColumn.setCellFactory(column -> new DateTimeCell());
        endColumn.setCellFactory(column -> new DateTimeCell());

        requestTable.setItems(requests);
        refresh();
    }

    @FXML
    private void refresh() {
        try {
            ownerNames = userService.getAll().stream()
                .collect(Collectors.toMap(User::getId, User::getName));
            plates = vehicleService.getAll().stream()
                .collect(Collectors.toMap(Vehicle::getId, Vehicle::getLicensePlate));
            spotNumbers = spotService.getAll().stream()
                .collect(Collectors.toMap(Spot::getId, Spot::getSpotNumber));
            requests.setAll(requestService.getAll());
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
        countLabel.setText("Всего заявок: " + requests.size());
    }

    private static class DateTimeCell extends TableCell<Request, LocalDateTime> {
        @Override
        protected void updateItem(LocalDateTime value, boolean empty) {
            super.updateItem(value, empty);
            setText(empty || value == null ? null : DATE_TIME_FORMAT.format(value));
        }
    }
}

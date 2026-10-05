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
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import ru.mirea.project.AppContext;
import ru.mirea.project.exception.BusinessException;
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

    private final AppContext context;
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
    @FXML
    private Button editButton;

    public RequestController(AppContext context) {
        this.context = context;
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
        requestTable.setRowFactory(table -> {
            TableRow<Request> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    edit();
                }
            });
            return row;
        });
        editButton.disableProperty().bind(requestTable.getSelectionModel().selectedItemProperty().isNull());
        refresh();
    }

    @FXML
    private void create() {
        openForm(null);
    }

    @FXML
    private void edit() {
        Request selected = requestTable.getSelectionModel().getSelectedItem();
        try {
            requestService.checkEditable(selected);
        } catch (BusinessException e) {
            Alerts.error(e);
            return;
        }
        openForm(selected);
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

    private void openForm(Request request) {
        try {
            FXMLLoader loader = Views.load("RequestFormDialog.fxml", context);
            RequestFormController form = loader.getController();
            form.prepare(request);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(request == null ? "Новая заявка" : "Изменение заявки");
            dialog.initOwner(requestTable.getScene().getWindow());
            dialog.setDialogPane(loader.getRoot());
            dialog.getDialogPane().getStylesheets().add(Views.stylesheet());
            dialog.showAndWait();

            form.getSavedRequest().ifPresent(saved -> {
                refresh();
                select(saved.getId());
            });
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }

    private void select(long requestId) {
        requests.stream()
            .filter(request -> request.getId() == requestId)
            .findFirst()
            .ifPresent(request -> {
                requestTable.getSelectionModel().select(request);
                requestTable.scrollTo(request);
            });
    }

    private static class DateTimeCell extends TableCell<Request, LocalDateTime> {
        @Override
        protected void updateItem(LocalDateTime value, boolean empty) {
            super.updateItem(value, empty);
            setText(empty || value == null ? null : DATE_TIME_FORMAT.format(value));
        }
    }
}

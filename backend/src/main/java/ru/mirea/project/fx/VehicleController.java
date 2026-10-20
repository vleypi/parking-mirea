package ru.mirea.project.fx;

import java.util.Map;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import ru.mirea.project.AppContext;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;

public class VehicleController {
    private final AppContext context;
    private final VehicleService vehicleService;
    private final UserService userService;

    private final ObservableList<Vehicle> vehicles = FXCollections.observableArrayList();
    private final SortedList<Vehicle> sortedVehicles = new SortedList<>(vehicles);
    private Map<Long, String> ownerNames = Map.of();

    @FXML
    private TableView<Vehicle> vehicleTable;
    @FXML
    private TableColumn<Vehicle, String> plateColumn;
    @FXML
    private TableColumn<Vehicle, String> regionColumn;
    @FXML
    private TableColumn<Vehicle, String> brandColumn;
    @FXML
    private TableColumn<Vehicle, String> modelColumn;
    @FXML
    private TableColumn<Vehicle, String> ownerColumn;
    @FXML
    private Label countLabel;
    @FXML
    private Button editButton;

    public VehicleController(AppContext context) {
        this.context = context;
        this.vehicleService = context.getVehicleService();
        this.userService = context.getUserService();
    }

    @FXML
    private void initialize() {
        plateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLicensePlate()));
        regionColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRegion()));
        brandColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBrand()));
        modelColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getModel()));
        ownerColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(ownerNames.getOrDefault(cell.getValue().getUserId(), "—")));

        sortedVehicles.comparatorProperty().bind(vehicleTable.comparatorProperty());
        vehicleTable.setItems(sortedVehicles);
        vehicleTable.setRowFactory(table -> {
            TableRow<Vehicle> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    edit();
                }
            });
            return row;
        });
        editButton.disableProperty().bind(vehicleTable.getSelectionModel().selectedItemProperty().isNull());
        refresh();
    }

    @FXML
    private void create() {
        openForm(null);
    }

    @FXML
    private void edit() {
        Vehicle selected = vehicleTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openForm(selected);
        }
    }

    @FXML
    private void refresh() {
        try {
            ownerNames = userService.getAll().stream()
                .collect(Collectors.toMap(User::getId, User::getName));
            vehicles.setAll(vehicleService.getAll());
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
        countLabel.setText("Всего автомобилей: " + vehicles.size());
    }

    private void openForm(Vehicle vehicle) {
        try {
            FXMLLoader loader = Views.load("VehicleFormDialog.fxml", context);
            VehicleFormController form = loader.getController();
            form.prepare(vehicle);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(vehicle == null ? "Новый автомобиль" : "Изменение автомобиля");
            dialog.initOwner(vehicleTable.getScene().getWindow());
            dialog.setDialogPane(loader.getRoot());
            dialog.getDialogPane().getStylesheets().add(Views.stylesheet());
            dialog.showAndWait();

            form.getSavedVehicle().ifPresent(saved -> {
                refresh();
                select(saved.getId());
            });
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }

    private void select(long vehicleId) {
        sortedVehicles.stream()
            .filter(vehicle -> vehicle.getId() == vehicleId)
            .findFirst()
            .ifPresent(vehicle -> {
                vehicleTable.getSelectionModel().select(vehicle);
                vehicleTable.scrollTo(vehicle);
            });
    }
}
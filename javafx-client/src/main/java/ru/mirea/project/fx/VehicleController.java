package ru.mirea.project.fx;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import ru.mirea.project.AppContext;
import ru.mirea.project.dto.filter.VehicleFilter;
import ru.mirea.project.dto.filter.VehicleFilter.SortField;
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

public class VehicleController {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String ERROR_STYLE = "field-error";
    private static final String ANY_VALUE = "Любой";
    private static final String NO_SORT = "Без сортировки";
    private static final long NO_ID = -1L;

    private final AppContext context;
    private final VehicleService vehicleService;
    private final UserService userService;
    private final RequestService requestService;
    private final SpotService spotService;

    private final ObservableList<Vehicle> vehicles = FXCollections.observableArrayList();
    private final SortedList<Vehicle> sortedVehicles = new SortedList<>(vehicles);
    private final ObservableList<Request> vehicleRequests = FXCollections.observableArrayList();
    private Map<Long, String> ownerNames = Map.of();
    private Map<Long, Integer> spotNumbers = Map.of();
    private VehicleFilter currentFilter = new VehicleFilter();
    private List<Control> filterFields = List.of();

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
    private Label placeholderLabel;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Button statisticsButton;
    @FXML
    private TextField plateSearchField;
    @FXML
    private TextField modelSearchField;
    @FXML
    private TextField regionField;
    @FXML
    private ComboBox<String> brandFilterBox;
    @FXML
    private ComboBox<User> ownerFilterBox;
    @FXML
    private ComboBox<SortField> sortFieldBox;
    @FXML
    private CheckBox descendingCheckBox;
    @FXML
    private Label filterErrorLabel;
    @FXML
    private TableView<Request> requestTable;
    @FXML
    private TableColumn<Request, Integer> requestSpotColumn;
    @FXML
    private TableColumn<Request, LocalDateTime> requestStartColumn;
    @FXML
    private TableColumn<Request, LocalDateTime> requestEndColumn;
    @FXML
    private TableColumn<Request, RequestStatus> requestStatusColumn;
    @FXML
    private Label requestPlaceholderLabel;

    public VehicleController(AppContext context) {
        this.context = context;
        this.vehicleService = context.getVehicleService();
        this.userService = context.getUserService();
        this.requestService = context.getRequestService();
        this.spotService = context.getSpotService();
    }

    @FXML
    private void initialize() {
        plateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLicensePlate()));
        regionColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getRegion()));
        regionColumn.setComparator(Comparator.comparingInt(Integer::parseInt));
        plateColumn.setComparator(Comparator.comparingInt((String plate) -> Integer.parseInt(plate.substring(1, 4)))
            .thenComparing(Comparator.naturalOrder()));
        brandColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBrand()));
        modelColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getModel()));
        ownerColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(ownerNames.getOrDefault(cell.getValue().getUserId(), "—")));

        // Сортировка №1: по клику на заголовок столбца. Выполняется в самом TableView, прямо в памяти.
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
        deleteButton.disableProperty().bind(vehicleTable.getSelectionModel().selectedItemProperty().isNull());
        statisticsButton.disableProperty().bind(vehicleTable.getSelectionModel().selectedItemProperty().isNull());

        initRequestTable();
        vehicleTable.getSelectionModel().selectedItemProperty()
            .addListener((observable, oldVehicle, newVehicle) -> showRequestsOf(newVehicle));
        initFilterPanel();
        refresh();
    }

    private void initRequestTable() {
        requestSpotColumn.setCellValueFactory(cell ->
            new SimpleObjectProperty<>(spotNumbers.get(cell.getValue().getSpotId())));
        requestStartColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getStartTime()));
        requestEndColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getEndTime()));
        requestStatusColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getStatus()));
        requestStartColumn.setCellFactory(column -> new DateTimeCell());
        requestEndColumn.setCellFactory(column -> new DateTimeCell());
        requestTable.setItems(vehicleRequests);
    }

    private void initFilterPanel() {
        filterFields = List.of(plateSearchField, modelSearchField, regionField, brandFilterBox, ownerFilterBox);

        brandFilterBox.setPromptText(ANY_VALUE);
        brandFilterBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(String brand) {
                return brand == null ? ANY_VALUE : brand;
            }

            @Override
            public String fromString(String text) {
                return null;
            }
        });
        ownerFilterBox.setPromptText(ANY_VALUE);
        ownerFilterBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(User owner) {
                return owner == null ? ANY_VALUE : owner.getName();
            }

            @Override
            public User fromString(String text) {
                return null;
            }
        });

        List<SortField> sortItems = new ArrayList<>(List.of(SortField.values()));
        sortItems.remove(SortField.ID);
        sortItems.add(0, null);
        sortFieldBox.getItems().setAll(sortItems);
        sortFieldBox.setPromptText(NO_SORT);
        sortFieldBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SortField field) {
                return field == null ? NO_SORT : field.getTitle();
            }

            @Override
            public SortField fromString(String text) {
                return null;
            }
        });
        sortFieldBox.valueProperty().addListener((observable, oldField, newField) -> applySort());
        descendingCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> applySort());

        filterErrorLabel.managedProperty().bind(filterErrorLabel.textProperty().isNotEmpty());
        filterErrorLabel.visibleProperty().bind(filterErrorLabel.textProperty().isNotEmpty());
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
    private void delete() {
        Vehicle selected = vehicleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        String description = "Удалить автомобиль?\n\n"
            + "Гос. номер: " + selected.getLicensePlate() + "\n"
            + "Марка: " + selected.getBrand() + "\n"
            + "Модель: " + selected.getModel() + "\n"
            + "Владелец: " + ownerNames.getOrDefault(selected.getUserId(), "?");
        if (!Alerts.confirm(description)) {
            return;
        }
        try {
            vehicleService.delete(selected.getId());
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
        refresh();
    }

    @FXML
    private void refresh() {
        long selectedId = selectedVehicleId();
        try {
            List<User> users = userService.getAll();
            ownerNames = users.stream().collect(Collectors.toMap(User::getId, User::getName));
            spotNumbers = spotService.getAll().stream()
                .collect(Collectors.toMap(Spot::getId, Spot::getSpotNumber));
            reloadFilterOptions(users, vehicleService.getAll());
            vehicles.setAll(vehicleService.find(currentFilter));
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
        updateStatus();
        if (selectedId != NO_ID) {
            select(selectedId);
        }
    }

    @FXML
    private void applyFilters() {
        clearFilterErrors();
        try {
            currentFilter = readFilter();
        } catch (BusinessException e) {
            filterErrorLabel.setText(e.getMessage());
            return;
        }
        refresh();
    }

    @FXML
    private void resetFilters() {
        plateSearchField.clear();
        modelSearchField.clear();
        regionField.clear();
        brandFilterBox.setValue(null);
        ownerFilterBox.setValue(null);
        clearFilterErrors();
        currentFilter = readFilter();
        refresh();
    }

        @FXML
    private void showParkedDuring() {
        try {
            FXMLLoader loader = Views.load("VehicleParkedDialog.fxml", context);
            showDialog(loader, "Автомобили, стоявшие на парковке в период");
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }

    @FXML
    private void showStatistics() {
        Vehicle selected = vehicleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            FXMLLoader loader = Views.load("VehicleStatisticsDialog.fxml", context);
            VehicleStatisticsController statistics = loader.getController();
            statistics.prepare(selected);
            showDialog(loader, "Статистика автомобиля");
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }

    // Сортировка №2: через сервис. Выбранное поле и направление попадают в VehicleFilter,
    // VehicleService.find(filter) сам сортирует список, а таблица получает уже готовый результат.
    private void applySort() {
        currentFilter.setSort(selectedSortField(), !descendingCheckBox.isSelected());
        vehicleTable.getSortOrder().clear();
        refresh();
    }

    private SortField selectedSortField() {
        return sortFieldBox.getValue() == null ? SortField.ID : sortFieldBox.getValue();
    }

    private VehicleFilter readFilter() {
        VehicleFilter filter = new VehicleFilter();
        if (!plateSearchField.getText().isBlank()) {
            filter.setPlateContains(plateSearchField.getText());
        }
        if (!modelSearchField.getText().isBlank()) {
            filter.setModelContains(modelSearchField.getText());
        }
        if (!regionField.getText().isBlank()) {
            try {
                filter.setRegion(regionField.getText().trim());
            } catch (BusinessException e) {
                throw fieldError(e.getMessage(), regionField);
            }
        }
        if (brandFilterBox.getValue() != null) {
            filter.setBrandContains(brandFilterBox.getValue());
        }
        if (ownerFilterBox.getValue() != null) {
            filter.setOwnerNameContains(ownerFilterBox.getValue().getName());
        }
        filter.setSort(selectedSortField(), !descendingCheckBox.isSelected());
        return filter;
    }

    private void reloadFilterOptions(List<User> users, List<Vehicle> allVehicles) {
        String selectedBrand = brandFilterBox.getValue();
        User selectedOwner = ownerFilterBox.getValue();

        List<String> brands = new ArrayList<>();
        brands.add(null);
        allVehicles.stream()
            .map(Vehicle::getBrand)
            .distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .forEach(brands::add);
        brandFilterBox.getItems().setAll(brands);
        brandFilterBox.setValue(selectedBrand != null && brands.contains(selectedBrand) ? selectedBrand : null);

        List<User> owners = new ArrayList<>();
        owners.add(null);
        owners.addAll(users);
        ownerFilterBox.getItems().setAll(owners);
        ownerFilterBox.setValue(selectedOwner == null ? null : users.stream()
            .filter(user -> user.getId() == selectedOwner.getId())
            .findFirst()
            .orElse(null));
    }

    private void showRequestsOf(Vehicle vehicle) {
        if (vehicle == null) {
            vehicleRequests.clear();
            requestPlaceholderLabel.setText("Выберите автомобиль в таблице выше");
            return;
        }
        try {
            vehicleRequests.setAll(requestService.getAll().stream()
                .filter(request -> request.getVehicleId() == vehicle.getId())
                .toList());
            requestPlaceholderLabel.setText("У этого автомобиля нет заявок");
        } catch (RuntimeException e) {
            vehicleRequests.clear();
            Alerts.error(e);
        }
    }

    private BusinessException fieldError(String message, Control... invalidFields) {
        for (Control field : invalidFields) {
            field.getStyleClass().add(ERROR_STYLE);
        }
        return new BusinessException(message);
    }

    private void clearFilterErrors() {
        filterErrorLabel.setText("");
        filterFields.forEach(field -> field.getStyleClass().remove(ERROR_STYLE));
    }

    private void updateStatus() {
        boolean filtering = !currentFilter.isEmpty();
        countLabel.setText((filtering ? "Найдено автомобилей: " : "Всего автомобилей: ") + vehicles.size());
        placeholderLabel.setText(filtering ? "Ничего не найдено" : "Автомобилей нет");
    }

    private void openForm(Vehicle vehicle) {
        try {
            FXMLLoader loader = Views.load("VehicleFormDialog.fxml", context);
            VehicleFormController form = loader.getController();
            form.prepare(vehicle);
            showDialog(loader, vehicle == null ? "Новый автомобиль" : "Изменение автомобиля");

            form.getSavedVehicle().ifPresent(saved -> {
                refresh();
                select(saved.getId());
            });
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }

    private void showDialog(FXMLLoader loader, String title) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.initOwner(vehicleTable.getScene().getWindow());
        dialog.setDialogPane(loader.getRoot());
        dialog.getDialogPane().getStylesheets().add(Views.stylesheet());
        dialog.showAndWait();
    }

    private long selectedVehicleId() {
        Vehicle selected = vehicleTable.getSelectionModel().getSelectedItem();
        return selected == null ? NO_ID : selected.getId();
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

    private static class DateTimeCell extends TableCell<Request, LocalDateTime> {
        @Override
        protected void updateItem(LocalDateTime value, boolean empty) {
            super.updateItem(value, empty);
            setText(empty || value == null ? null : DATE_TIME_FORMAT.format(value));
        }
    }
}
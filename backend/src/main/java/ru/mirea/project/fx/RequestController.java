package ru.mirea.project.fx;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import ru.mirea.project.AppContext;
import ru.mirea.project.dto.filter.RequestFilter;
import ru.mirea.project.dto.filter.RequestFilter.SortField;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.entity.Request;
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.model.enums.LookupValue;
import ru.mirea.project.model.enums.RequestStatus;
import ru.mirea.project.model.enums.SpotType;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.service.RequestService;
import ru.mirea.project.service.SpotService;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;

public class RequestController {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String ERROR_STYLE = "field-error";
    private static final String ANY_VALUE = "Любой";
    private static final String NO_SORT = "Без сортировки";

    private final AppContext context;
    private final RequestService requestService;
    private final UserService userService;
    private final VehicleService vehicleService;
    private final SpotService spotService;

    private final ObservableList<Request> requests = FXCollections.observableArrayList();
    private final SortedList<Request> sortedRequests = new SortedList<>(requests);
    private Map<Long, String> ownerNames = Map.of();
    private Map<Long, String> plates = Map.of();
    private Map<Long, Integer> spotNumbers = Map.of();
    private RequestFilter currentFilter = new RequestFilter();
    private List<Control> filterFields = List.of();

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
    @FXML
    private Button deleteButton;
    @FXML
    private Button detailsButton;
    @FXML
    private Label placeholderLabel;
    @FXML
    private TextField ownerSearchField;
    @FXML
    private TextField plateSearchField;
    @FXML
    private ComboBox<RequestStatus> statusFilterBox;
    @FXML
    private ComboBox<SpotType> spotTypeFilterBox;
    @FXML
    private TextField spotNumberField;
    @FXML
    private DatePicker periodFromPicker;
    @FXML
    private DatePicker periodToPicker;
    @FXML
    private ComboBox<SortField> sortFieldBox;
    @FXML
    private CheckBox descendingCheckBox;
    @FXML
    private Label filterErrorLabel;

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

        sortedRequests.comparatorProperty().bind(requestTable.comparatorProperty());
        requestTable.setItems(sortedRequests);
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
        deleteButton.disableProperty().bind(requestTable.getSelectionModel().selectedItemProperty().isNull());
        detailsButton.disableProperty().bind(requestTable.getSelectionModel().selectedItemProperty().isNull());
        initFilterPanel();
        refresh();
    }

    private void initFilterPanel() {
        filterFields = List.of(ownerSearchField, plateSearchField, statusFilterBox, spotTypeFilterBox,
            spotNumberField, periodFromPicker, periodToPicker);

        fillWithAnyValue(statusFilterBox, RequestStatus.values());
        fillWithAnyValue(spotTypeFilterBox, SpotType.values());

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
    private void delete() {
        Request selected = requestTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        String description = "Удалить заявку?\n\n"
            + "Владелец: " + ownerNames.getOrDefault(selected.getUserId(), "?") + "\n"
            + "Автомобиль: " + plates.getOrDefault(selected.getVehicleId(), "?") + "\n"
            + "Место: " + spotNumbers.get(selected.getSpotId()) + "\n"
            + "Период: с " + DATE_TIME_FORMAT.format(selected.getStartTime())
            + " по " + DATE_TIME_FORMAT.format(selected.getEndTime()) + "\n"
            + "Статус: " + selected.getStatus();
        if (!Alerts.confirm(description)) {
            return;
        }
        try {
            requestService.delete(selected.getId());
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
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
            requests.setAll(requestService.find(currentFilter));
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
        updateStatus();
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
        ownerSearchField.clear();
        plateSearchField.clear();
        statusFilterBox.setValue(null);
        spotTypeFilterBox.setValue(null);
        spotNumberField.clear();
        periodFromPicker.setValue(null);
        periodToPicker.setValue(null);
        clearFilterErrors();
        currentFilter = readFilter();
        refresh();
    }

    private void applySort() {
        currentFilter.setSort(selectedSortField(), !descendingCheckBox.isSelected());
        requestTable.getSortOrder().clear();
        refresh();
    }

    private SortField selectedSortField() {
        return sortFieldBox.getValue() == null ? SortField.ID : sortFieldBox.getValue();
    }

    private RequestFilter readFilter() {
        RequestFilter filter = new RequestFilter();
        if (!ownerSearchField.getText().isBlank()) {
            filter.setOwnerNameContains(ownerSearchField.getText());
        }
        if (!plateSearchField.getText().isBlank()) {
            filter.setPlateContains(plateSearchField.getText());
        }
        if (statusFilterBox.getValue() != null) {
            filter.setStatus(statusFilterBox.getValue());
        }
        if (spotTypeFilterBox.getValue() != null) {
            filter.setSpotType(spotTypeFilterBox.getValue());
        }
        if (!spotNumberField.getText().isBlank()) {
            filter.setSpotNumber(readSpotNumber());
        }
        filter.setPeriod(readPeriod());
        filter.setSort(selectedSortField(), !descendingCheckBox.isSelected());
        return filter;
    }

    private int readSpotNumber() {
        try {
            return spotService.parseSpotNumber(spotNumberField.getText());
        } catch (BusinessException e) {
            throw fieldError(e.getMessage(), spotNumberField);
        }
    }

    private Period readPeriod() {
        LocalDate fromDate = periodFromPicker.getValue();
        LocalDate toDate = periodToPicker.getValue();
        LocalDateTime from = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? null : toDate.atTime(LocalTime.MAX);
        try {
            return new Period(from, to);
        } catch (BusinessException e) {
            throw fieldError(e.getMessage(), periodFromPicker, periodToPicker);
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
        countLabel.setText((filtering ? "Найдено заявок: " : "Всего заявок: ") + requests.size());
        placeholderLabel.setText(filtering ? "Ничего не найдено" : "Заявок нет");
    }

    @FXML
    private void showDetails() {
        Request selected = requestTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            FXMLLoader loader = Views.load("RequestDetailsDialog.fxml", context);
            RequestDetailsController details = loader.getController();
            details.prepare(selected);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Заявка: подробные данные");
            dialog.initOwner(requestTable.getScene().getWindow());
            dialog.setDialogPane(loader.getRoot());
            dialog.getDialogPane().getStylesheets().add(Views.stylesheet());
            dialog.showAndWait();
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
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

    private static <T extends LookupValue> void fillWithAnyValue(ComboBox<T> box, T[] values) {
        List<T> items = new ArrayList<>();
        items.add(null);
        items.addAll(List.of(values));
        box.getItems().setAll(items);
        box.setPromptText(ANY_VALUE);
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? ANY_VALUE : value.getTitle();
            }

            @Override
            public T fromString(String text) {
                return null;
            }
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

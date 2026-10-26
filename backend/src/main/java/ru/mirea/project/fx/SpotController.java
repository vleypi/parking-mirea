package ru.mirea.project.fx;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import ru.mirea.project.AppContext;
import ru.mirea.project.dto.filter.RequestFilter;
import ru.mirea.project.dto.filter.SpotFilter;
import ru.mirea.project.dto.filter.SpotFilter.SortField;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.entity.Request;
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.model.enums.RequestStatus;
import ru.mirea.project.model.enums.SpotType;
import ru.mirea.project.model.value.NumberRange;
import ru.mirea.project.service.ExportService;
import ru.mirea.project.service.RequestService;
import ru.mirea.project.service.SpotService;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;

public class SpotController {
    private static final String ERROR_STYLE = "field-error";
    private static final String ANY_TYPE = "Любой тип";
    private static final String NO_SORT = "Без сортировки";
    private static final String BUSY_STYLE = "occupancy-busy";
    private static final String FREE_STYLE = "occupancy-free";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final AppContext context;
    private final SpotService spotService;
    private final ExportService exportService;
    private final RequestService requestService;
    private final UserService userService;
    private final VehicleService vehicleService;

    private final ObservableList<Request> relatedRequests = FXCollections.observableArrayList();
    private Map<Long, String> ownerNames = Map.of();
    private Map<Long, String> plates = Map.of();

    private final ObservableList<Spot> spots = FXCollections.observableArrayList();
    private final FilteredList<Spot> searchedSpots = new FilteredList<>(spots);
    private final SortedList<Spot> sortedSpots = new SortedList<>(searchedSpots);

    private SpotFilter currentFilter = new SpotFilter();
    private List<Control> rangeFields = List.of();
    private boolean resetting;

    @FXML
    private TableView<Spot> spotTable;
    @FXML
    private TableColumn<Spot, Integer> numberColumn;
    @FXML
    private TableColumn<Spot, String> typeColumn;
    @FXML
    private TableColumn<Spot, BigDecimal> rateColumn;
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
    private TextField searchField;
    @FXML
    private ComboBox<SpotType> typeFilterBox;
    @FXML
    private TextField numberFromField;
    @FXML
    private TextField numberToField;
    @FXML
    private TextField rateFromField;
    @FXML
    private TextField rateToField;
    @FXML
    private ComboBox<SortField> sortFieldBox;
    @FXML
    private CheckBox descendingCheckBox;
    @FXML
    private Label filterErrorLabel;
    @FXML
    private Label relatedTitleLabel;
    @FXML
    private Label occupancyLabel;
    @FXML
    private Label relatedPlaceholderLabel;
    @FXML
    private TableView<Request> relatedTable;
    @FXML
    private TableColumn<Request, String> relatedOwnerColumn;
    @FXML
    private TableColumn<Request, String> relatedPlateColumn;
    @FXML
    private TableColumn<Request, LocalDateTime> relatedStartColumn;
    @FXML
    private TableColumn<Request, LocalDateTime> relatedEndColumn;
    @FXML
    private TableColumn<Request, RequestStatus> relatedStatusColumn;

    public SpotController(AppContext context) {
        this.context = context;
        this.spotService = context.getSpotService();
        this.exportService = context.getExportService();
        this.requestService = context.getRequestService();
        this.userService = context.getUserService();
        this.vehicleService = context.getVehicleService();
    }

    @FXML
    private void initialize() {
        numberColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getSpotNumber()));
        typeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSpotType().getTitle()));
        rateColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getHourlyRate()));
        rateColumn.setCellFactory(column -> new RateCell());

        sortedSpots.comparatorProperty().bind(spotTable.comparatorProperty());
        spotTable.setItems(sortedSpots);
        spotTable.setRowFactory(table -> {
            TableRow<Spot> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    edit();
                }
            });
            return row;
        });
        editButton.disableProperty().bind(spotTable.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(spotTable.getSelectionModel().selectedItemProperty().isNull());
        statisticsButton.disableProperty().bind(spotTable.getSelectionModel().selectedItemProperty().isNull());

        initRelatedPanel();
        initFilterPanel();
        refresh();
    }

    private void initRelatedPanel() {
        relatedOwnerColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(ownerNames.getOrDefault(cell.getValue().getUserId(), "?")));
        relatedPlateColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(plates.getOrDefault(cell.getValue().getVehicleId(), "?")));
        relatedStartColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getStartTime()));
        relatedEndColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getEndTime()));
        relatedStatusColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getStatus()));
        relatedStartColumn.setCellFactory(column -> new DateTimeCell());
        relatedEndColumn.setCellFactory(column -> new DateTimeCell());
        relatedTable.setItems(relatedRequests);

        spotTable.getSelectionModel().selectedItemProperty()
            .addListener((observable, oldSpot, newSpot) -> showRelated(newSpot));
        showRelated(null);
    }

    private void showRelated(Spot spot) {
        occupancyLabel.getStyleClass().removeAll(BUSY_STYLE, FREE_STYLE);
        if (spot == null) {
            relatedRequests.clear();
            relatedTitleLabel.setText("Заявки на место");
            occupancyLabel.setText("");
            relatedPlaceholderLabel.setText("Выберите место в таблице выше");
            return;
        }
        try {
            ownerNames = userService.getAll().stream()
                .collect(Collectors.toMap(User::getId, User::getName));
            plates = vehicleService.getAll().stream()
                .collect(Collectors.toMap(Vehicle::getId, Vehicle::getLicensePlate));

            RequestFilter filter = new RequestFilter();
            filter.setSpotNumber(spot.getSpotNumber());
            filter.setSort(RequestFilter.SortField.START, false);
            relatedRequests.setAll(requestService.find(filter));

            boolean occupied = spotService.isOccupiedNow(spot.getId());
            occupancyLabel.setText(occupied ? "Занято сейчас" : "Свободно сейчас");
            occupancyLabel.getStyleClass().add(occupied ? BUSY_STYLE : FREE_STYLE);
            relatedTitleLabel.setText("Заявки на место №" + spot.getSpotNumber()
                + " (" + relatedRequests.size() + ")");
            relatedPlaceholderLabel.setText("На это место заявок нет");
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }

    private void initFilterPanel() {
        rangeFields = List.of(numberFromField, numberToField, rateFromField, rateToField);

        List<SpotType> typeItems = new ArrayList<>();
        typeItems.add(null);
        typeItems.addAll(List.of(SpotType.values()));
        typeFilterBox.getItems().setAll(typeItems);
        typeFilterBox.setPromptText(ANY_TYPE);
        typeFilterBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SpotType type) {
                return type == null ? ANY_TYPE : type.getTitle();
            }

            @Override
            public SpotType fromString(String text) {
                return null;
            }
        });
        typeFilterBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (!resetting) {
                applyFilters();
            }
        });

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            searchedSpots.setPredicate(createSearchPredicate(newValue));
            updateStatus();
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
        Spot selected = spotTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openForm(selected);
        }
    }

    @FXML
    private void showFreeSpots() {
        try {
            FXMLLoader loader = Views.load("SpotFreeDialog.fxml", context);
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Свободные места на период");
            dialog.initOwner(spotTable.getScene().getWindow());
            dialog.setDialogPane(loader.getRoot());
            dialog.getDialogPane().getStylesheets().add(Views.stylesheet());
            dialog.showAndWait();
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }

    @FXML
    private void showStatistics() {
        Spot selected = spotTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            FXMLLoader loader = Views.load("SpotStatisticsDialog.fxml", context);
            SpotStatisticsController statistics = loader.getController();
            statistics.prepare(selected);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Статистика места");
            dialog.initOwner(spotTable.getScene().getWindow());
            dialog.setDialogPane(loader.getRoot());
            dialog.getDialogPane().getStylesheets().add(Views.stylesheet());
            dialog.showAndWait();
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }

    @FXML
    private void export() {
        if (spotTable.getItems().isEmpty()) {
            Alerts.info("Экспорт", "В таблице нет строк, экспортировать нечего");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт парковочных мест");
        chooser.setInitialFileName("Места.xlsx");
        chooser.getExtensionFilters().setAll(
            new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"),
            new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        File file = chooser.showSaveDialog(spotTable.getScene().getWindow());
        if (file == null) {
            return;
        }
        exportTo(withExtension(file, chooser.getSelectedExtensionFilter()));
    }

    private void exportTo(Path file) {
        List<TableColumn<Spot, ?>> columns = spotTable.getColumns();
        List<String> headers = columns.stream()
            .map(TableColumn::getText)
            .toList();
        List<List<Object>> rows = spotTable.getItems().stream()
            .map(spot -> columns.stream()
                .<Object>map(column -> column.getCellData(spot))
                .toList())
            .toList();
        try {
            exportService.export("Места", headers, rows, file);
            Alerts.info("Экспорт", "Сохранено строк: " + rows.size() + "\nФайл: " + file);
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }

    private Path withExtension(File file, FileChooser.ExtensionFilter filter) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".xlsx") || name.endsWith(".csv")) {
            return file.toPath();
        }
        String extension = filter != null && filter.getExtensions().contains("*.csv") ? ".csv" : ".xlsx";
        return file.toPath().resolveSibling(file.getName() + extension);
    }

    @FXML
    private void delete() {
        Spot selected = spotTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        String description = "Удалить парковочное место №" + selected.getSpotNumber()
            + " (" + selected.getSpotType().getTitle()
            + ", тариф " + selected.getHourlyRate().toPlainString() + " руб/ч)?";
        if (!Alerts.confirm(description)) {
            return;
        }
        try {
            spotService.delete(selected.getId());
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
        refresh();
    }

    @FXML
    private void refresh() {
        Spot selected = spotTable.getSelectionModel().getSelectedItem();
        try {
            spots.setAll(spotService.find(currentFilter));
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
        updateStatus();
        if (selected != null) {
            select(selected.getId());
        }
    }

    @FXML
    private void applyFilters() {
        clearFilterErrors();
        try {
            SpotFilter filter = createFilter();
            SpotType type = typeFilterBox.getValue();
            if (type != null) {
                filter.setType(type);
            }
            filter.setNumberRange(readRange(numberFromField, numberToField, true));
            filter.setRateRange(readRange(rateFromField, rateToField, false));
            currentFilter = filter;
        } catch (BusinessException e) {
            filterErrorLabel.setText(e.getMessage());
            return;
        }
        refresh();
    }

    @FXML
    private void resetFilters() {
        resetting = true;
        try {
            searchField.clear();
            typeFilterBox.setValue(null);
            numberFromField.clear();
            numberToField.clear();
            rateFromField.clear();
            rateToField.clear();
        } finally {
            resetting = false;
        }
        clearFilterErrors();
        currentFilter = createFilter();
        refresh();
    }

    private void applySort() {
        currentFilter.setSort(selectedSortField(), !descendingCheckBox.isSelected());
        spotTable.getSortOrder().clear();
        refresh();
    }

    private SpotFilter createFilter() {
        SpotFilter filter = new SpotFilter();
        filter.setSort(selectedSortField(), !descendingCheckBox.isSelected());
        return filter;
    }

    private SortField selectedSortField() {
        return sortFieldBox.getValue() == null ? SortField.ID : sortFieldBox.getValue();
    }

    private NumberRange readRange(TextField fromField, TextField toField, boolean integer) {
        BigDecimal min = readBound(fromField, integer);
        BigDecimal max = readBound(toField, integer);
        try {
            return new NumberRange(min, max);
        } catch (BusinessException e) {
            throw fieldError(e.getMessage(), fromField, toField);
        }
    }

    private BigDecimal readBound(TextField field, boolean integer) {
        if (field.getText() == null || field.getText().isBlank()) {
            return null;
        }
        try {
            if (integer) {
                return BigDecimal.valueOf(spotService.parseSpotNumber(field.getText()));
            }
            return spotService.parseHourlyRate(field.getText());
        } catch (BusinessException e) {
            throw fieldError(e.getMessage(), field);
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
        rangeFields.forEach(field -> field.getStyleClass().remove(ERROR_STYLE));
    }

    private Predicate<Spot> createSearchPredicate(String text) {
        String query = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return spot -> true;
        }
        return spot -> String.valueOf(spot.getSpotNumber()).contains(query)
            || spot.getSpotType().getTitle().toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean isFiltering() {
        return !currentFilter.isEmpty() || !searchField.getText().isBlank();
    }

    private void updateStatus() {
        boolean filtering = isFiltering();
        countLabel.setText((filtering ? "Найдено мест: " : "Всего мест: ") + searchedSpots.size());
        placeholderLabel.setText(filtering ? "Ничего не найдено" : "Парковочных мест нет");
    }

    private void openForm(Spot spot) {
        try {
            FXMLLoader loader = Views.load("SpotFormDialog.fxml", context);
            SpotFormController form = loader.getController();
            form.prepare(spot);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(spot == null ? "Новое парковочное место" : "Изменение парковочного места");
            dialog.initOwner(spotTable.getScene().getWindow());
            dialog.setDialogPane(loader.getRoot());
            dialog.getDialogPane().getStylesheets().add(Views.stylesheet());
            dialog.showAndWait();

            form.getSavedSpot().ifPresent(saved -> {
                refresh();
                select(saved.getId());
            });
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }

    private void select(long spotId) {
        sortedSpots.stream()
            .filter(spot -> spot.getId() == spotId)
            .findFirst()
            .ifPresent(spot -> {
                spotTable.getSelectionModel().select(spot);
                spotTable.scrollTo(spot);
            });
    }

    private static class RateCell extends TableCell<Spot, BigDecimal> {
        @Override
        protected void updateItem(BigDecimal value, boolean empty) {
            super.updateItem(value, empty);
            setText(empty || value == null ? null : value.toPlainString());
        }
    }

    private static class DateTimeCell extends TableCell<Request, LocalDateTime> {
        @Override
        protected void updateItem(LocalDateTime value, boolean empty) {
            super.updateItem(value, empty);
            setText(empty || value == null ? null : DATE_TIME_FORMAT.format(value));
        }
    }
}

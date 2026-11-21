package ru.mirea.project.fx;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
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
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import ru.mirea.project.AppContext;
import ru.mirea.project.dto.filter.UserFilter;
import ru.mirea.project.dto.filter.UserFilter.SortField;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.entity.Request;
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.model.enums.RequestStatus;
import ru.mirea.project.model.value.NumberRange;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.service.ExportService;
import ru.mirea.project.service.RequestService;
import ru.mirea.project.service.SpotService;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;

/**
 * Контроллер вкладки «Владельцы» (UserView.fxml).
 * Цепочка: UserView.fxml -> UserController -> UserService -> UserRepository -> таблица users.
 * В контроллере нет SQL: только вызовы сервисов и работа с элементами интерфейса.
 */
public class UserController {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String ERROR_STYLE = "field-error";
    private static final String ANY_VALUE = "Любые";
    private static final String NO_SORT = "Без сортировки";
    private static final long NO_ID = -1L;

    private final AppContext context;
    private final UserService userService;
    private final VehicleService vehicleService;
    private final RequestService requestService;
    private final SpotService spotService;
    private final ExportService exportService;

    // ObservableList: TableView подписан на этот список и перерисовывается сам при любом его изменении.
    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final SortedList<User> sortedUsers = new SortedList<>(users);
    private final ObservableList<Vehicle> userVehicles = FXCollections.observableArrayList();
    private final ObservableList<Request> userRequests = FXCollections.observableArrayList();

    private Map<Long, Long> vehicleCounts = Map.of();
    private Map<Long, String> plates = Map.of();
    private Map<Long, Integer> spotNumbers = Map.of();
    private UserFilter currentFilter = new UserFilter();
    private List<Control> filterFields = List.of();

    // @FXML связывает поле с элементом из FXML, у которого такой же fx:id
    @FXML
    private TableView<User> userTable;
    @FXML
    private TableColumn<User, String> nameColumn;
    @FXML
    private TableColumn<User, String> phoneColumn;
    @FXML
    private TableColumn<User, Long> vehicleCountColumn;
    @FXML
    private TableColumn<User, LocalDateTime> registeredColumn;
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
    private TextField nameSearchField;
    @FXML
    private TextField phoneSearchField;
    @FXML
    private TextField vehiclesFromField;
    @FXML
    private TextField vehiclesToField;
    @FXML
    private ComboBox<Boolean> activeRequestsBox;
    @FXML
    private DatePicker registeredFromPicker;
    @FXML
    private DatePicker registeredToPicker;
    @FXML
    private ComboBox<SortField> sortFieldBox;
    @FXML
    private CheckBox descendingCheckBox;
    @FXML
    private Label filterErrorLabel;

    @FXML
    private TableView<Vehicle> vehicleTable;
    @FXML
    private TableColumn<Vehicle, String> vehiclePlateColumn;
    @FXML
    private TableColumn<Vehicle, String> vehicleBrandColumn;
    @FXML
    private TableColumn<Vehicle, String> vehicleModelColumn;
    @FXML
    private Label vehiclePlaceholderLabel;

    @FXML
    private TableView<Request> requestTable;
    @FXML
    private TableColumn<Request, String> requestPlateColumn;
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

    // Сервисы приходят через конструктор из AppContext (контроллер создаёт Views.createController)
    public UserController(AppContext context) {
        this.context = context;
        this.userService = context.getUserService();
        this.vehicleService = context.getVehicleService();
        this.requestService = context.getRequestService();
        this.spotService = context.getSpotService();
        this.exportService = context.getExportService();
    }

    // Вызывается FXMLLoader автоматически, когда все @FXML-поля уже заполнены
    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        phoneColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getPhone()));
        vehicleCountColumn.setCellValueFactory(cell ->
            new SimpleObjectProperty<>(vehicleCounts.getOrDefault(cell.getValue().getId(), 0L)));
        registeredColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getCreatedAt()));
        registeredColumn.setCellFactory(column -> new DateTimeCell<>());

        // Сортировка №1: по клику на заголовок столбца. Выполняет сам TableView в памяти:
        // SortedList берёт компаратор из таблицы и пересортировывает уже загруженные строки.
        sortedUsers.comparatorProperty().bind(userTable.comparatorProperty());
        userTable.setItems(sortedUsers);
        userTable.setRowFactory(table -> {
            TableRow<User> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    edit();
                }
            });
            return row;
        });
        editButton.disableProperty().bind(userTable.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(userTable.getSelectionModel().selectedItemProperty().isNull());
        statisticsButton.disableProperty().bind(userTable.getSelectionModel().selectedItemProperty().isNull());

        initRelatedTables();
        userTable.getSelectionModel().selectedItemProperty()
            .addListener((observable, oldUser, newUser) -> showRelatedOf(newUser));
        initFilterPanel();
        refresh();
    }

    private void initRelatedTables() {
        vehiclePlateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getLicensePlate()));
        vehicleBrandColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getBrand()));
        vehicleModelColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getModel()));
        vehicleTable.setItems(userVehicles);

        requestPlateColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(plates.getOrDefault(cell.getValue().getVehicleId(), "—")));
        requestSpotColumn.setCellValueFactory(cell ->
            new SimpleObjectProperty<>(spotNumbers.get(cell.getValue().getSpotId())));
        requestStartColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getStartTime()));
        requestEndColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getEndTime()));
        requestStatusColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getStatus()));
        requestStartColumn.setCellFactory(column -> new DateTimeCell<>());
        requestEndColumn.setCellFactory(column -> new DateTimeCell<>());
        requestTable.setItems(userRequests);
    }

    private void initFilterPanel() {
        filterFields = List.of(nameSearchField, phoneSearchField, vehiclesFromField, vehiclesToField,
            registeredFromPicker, registeredToPicker);

        activeRequestsBox.getItems().setAll(Arrays.asList(null, Boolean.TRUE, Boolean.FALSE));
        activeRequestsBox.setPromptText(ANY_VALUE);
        activeRequestsBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Boolean value) {
                if (value == null) {
                    return ANY_VALUE;
                }
                return value ? "Есть" : "Нет";
            }

            @Override
            public Boolean fromString(String text) {
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

    // ---------- CRUD ----------

    @FXML
    private void create() {
        openForm(null);
    }

    @FXML
    private void edit() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openForm(selected);
        }
    }

    @FXML
    private void delete() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        String description = "Удалить владельца?\n\n"
            + "Имя: " + selected.getName() + "\n"
            + "Телефон: " + selected.getPhone() + "\n"
            + "Автомобилей: " + vehicleCounts.getOrDefault(selected.getId(), 0L) + "\n"
            + "Зарегистрирован: " + DATE_TIME_FORMAT.format(selected.getCreatedAt());
        if (!Alerts.confirm(description)) {
            return;
        }
        try {
            userService.delete(selected.getId());
        } catch (RuntimeException e) {
            // Если у владельца есть машины или заявки, PostgreSQL вернёт ошибку 23503 (нарушение внешнего ключа).
            // UserRepository превращает её в DataAccessException с понятным текстом — показываем его в Alert.
            Alerts.error(e);
        }
        refresh();
    }

    @FXML
    private void refresh() {
        long selectedId = selectedUserId();
        try {
            List<Vehicle> allVehicles = vehicleService.getAll();
            vehicleCounts = allVehicles.stream()
                .collect(Collectors.groupingBy(Vehicle::getUserId, Collectors.counting()));
            plates = allVehicles.stream()
                .collect(Collectors.toMap(Vehicle::getId, Vehicle::getLicensePlate));
            spotNumbers = spotService.getAll().stream()
                .collect(Collectors.toMap(Spot::getId, Spot::getSpotNumber));
            users.setAll(userService.find(currentFilter));
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
        updateStatus();
        if (selectedId != NO_ID) {
            select(selectedId);
        }
        showRelatedOf(userTable.getSelectionModel().getSelectedItem());
    }

    // ---------- Поиск и фильтры ----------

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
        nameSearchField.clear();
        phoneSearchField.clear();
        vehiclesFromField.clear();
        vehiclesToField.clear();
        activeRequestsBox.setValue(null);
        registeredFromPicker.setValue(null);
        registeredToPicker.setValue(null);
        clearFilterErrors();
        currentFilter = readFilter();
        refresh();
    }

    // Все заполненные поля попадают в один UserFilter, поэтому фильтры работают одновременно
    private UserFilter readFilter() {
        UserFilter filter = new UserFilter();
        if (!nameSearchField.getText().isBlank()) {
            filter.setNameContains(nameSearchField.getText());
        }
        if (!phoneSearchField.getText().isBlank()) {
            try {
                filter.setPhoneDigits(phoneSearchField.getText());
            } catch (BusinessException e) {
                throw fieldError("Телефон: " + e.getMessage(), phoneSearchField);
            }
        }
        BigDecimal vehiclesFrom = readCount(vehiclesFromField, "Машин от");
        BigDecimal vehiclesTo = readCount(vehiclesToField, "Машин до");
        try {
            filter.setVehicleCount(new NumberRange(vehiclesFrom, vehiclesTo));
        } catch (BusinessException e) {
            throw fieldError("Количество машин: " + e.getMessage(), vehiclesFromField, vehiclesToField);
        }
        if (activeRequestsBox.getValue() != null) {
            filter.setHasActiveRequests(activeRequestsBox.getValue());
        }
        try {
            filter.setRegistered(readPeriod(registeredFromPicker, registeredToPicker));
        } catch (BusinessException e) {
            throw fieldError("Дата регистрации: " + e.getMessage(), registeredFromPicker, registeredToPicker);
        }
        filter.setSort(selectedSortField(), !descendingCheckBox.isSelected());
        return filter;
    }

    private BigDecimal readCount(TextField field, String title) {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            int value = Integer.parseInt(text);
            if (value < 0) {
                throw fieldError(title + ": число не может быть отрицательным", field);
            }
            return BigDecimal.valueOf(value);
        } catch (NumberFormatException e) {
            throw fieldError(title + ": введите целое число, например 2", field);
        }
    }

    static Period readPeriod(DatePicker fromPicker, DatePicker toPicker) {
        LocalDate fromDate = fromPicker.getValue();
        LocalDate toDate = toPicker.getValue();
        LocalDateTime from = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? null : toDate.atTime(LocalTime.MAX);
        return new Period(from, to);
    }

    // ---------- Сортировка ----------

    // Сортировка №2: через сервис. Поле и направление кладутся в UserFilter,
    // UserService.find(filter) сортирует список сам, а таблица получает готовый результат.
    // Сортировку по заголовку при этом сбрасываем, чтобы она не перекрывала порядок из сервиса.
    private void applySort() {
        currentFilter.setSort(selectedSortField(), !descendingCheckBox.isSelected());
        userTable.getSortOrder().clear();
        refresh();
    }

    private SortField selectedSortField() {
        return sortFieldBox.getValue() == null ? SortField.ID : sortFieldBox.getValue();
    }

    // ---------- Связанные данные ----------

    private void showRelatedOf(User user) {
        if (user == null) {
            userVehicles.clear();
            userRequests.clear();
            vehiclePlaceholderLabel.setText("Выберите владельца в таблице выше");
            requestPlaceholderLabel.setText("Выберите владельца в таблице выше");
            return;
        }
        try {
            userVehicles.setAll(vehicleService.getByUserId(user.getId()));
            userRequests.setAll(requestService.getAll().stream()
                .filter(request -> request.getUserId() == user.getId())
                .toList());
            vehiclePlaceholderLabel.setText("У владельца нет автомобилей");
            requestPlaceholderLabel.setText("У владельца нет заявок");
        } catch (RuntimeException e) {
            userVehicles.clear();
            userRequests.clear();
            Alerts.error(e);
        }
    }

    // ---------- Статистика ----------

    @FXML
    private void showStatistics() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        try {
            FXMLLoader loader = Views.load("UserStatisticsDialog.fxml", context);
            UserStatisticsController statistics = loader.getController();
            statistics.prepare(selected);
            showDialog(loader, "Статистика владельца");
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }

    // ---------- Экспорт ----------

    @FXML
    private void export() {
        if (userTable.getItems().isEmpty()) {
            Alerts.info("Экспорт", "В таблице нет строк, экспортировать нечего");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт владельцев");
        chooser.setInitialFileName("Владельцы.xlsx");
        chooser.getExtensionFilters().setAll(
            new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"),
            new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        File file = chooser.showSaveDialog(userTable.getScene().getWindow());
        if (file == null) {
            return;
        }
        exportTo(withExtension(file, chooser.getSelectedExtensionFilter()));
    }

    // Берём ровно то, что видно в таблице: те же столбцы и те же строки в том же порядке
    private void exportTo(Path file) {
        List<TableColumn<User, ?>> columns = userTable.getColumns();
        List<String> headers = columns.stream()
            .map(TableColumn::getText)
            .toList();
        List<List<Object>> rows = userTable.getItems().stream()
            .map(user -> columns.stream()
                .<Object>map(column -> column.getCellData(user))
                .toList())
            .toList();
        try {
            exportService.export("Владельцы", headers, rows, file);
            Alerts.info("Экспорт", "Сохранено строк: " + rows.size() + "\nФайл: " + file);
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }

    private Path withExtension(File file, FileChooser.ExtensionFilter filter) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".xlsx") || name.endsWith(".csv")) {
            return file.toPath();
        }
        String extension = filter != null && filter.getExtensions().contains("*.csv") ? ".csv" : ".xlsx";
        return file.toPath().resolveSibling(file.getName() + extension);
    }

    // ---------- Вспомогательное ----------

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
        countLabel.setText((filtering ? "Найдено владельцев: " : "Всего владельцев: ") + users.size());
        placeholderLabel.setText(filtering ? "Ничего не найдено" : "Владельцев нет");
    }

    private void openForm(User user) {
        try {
            FXMLLoader loader = Views.load("UserFormDialog.fxml", context);
            UserFormController form = loader.getController();
            form.prepare(user);
            showDialog(loader, user == null ? "Новый владелец" : "Изменение владельца");

            form.getSavedUser().ifPresent(saved -> {
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
        dialog.initOwner(userTable.getScene().getWindow());
        dialog.setDialogPane(loader.getRoot());
        dialog.getDialogPane().getStylesheets().add(Views.stylesheet());
        dialog.showAndWait();
    }

    private long selectedUserId() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        return selected == null ? NO_ID : selected.getId();
    }

    private void select(long userId) {
        sortedUsers.stream()
            .filter(user -> user.getId() == userId)
            .findFirst()
            .ifPresent(user -> {
                userTable.getSelectionModel().select(user);
                userTable.scrollTo(user);
            });
    }

    private static class DateTimeCell<S> extends TableCell<S, LocalDateTime> {
        @Override
        protected void updateItem(LocalDateTime value, boolean empty) {
            super.updateItem(value, empty);
            setText(empty || value == null ? null : DATE_TIME_FORMAT.format(value));
        }
    }
}

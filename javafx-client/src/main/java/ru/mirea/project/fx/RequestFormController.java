package ru.mirea.project.fx;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import ru.mirea.project.AppContext;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.entity.Request;
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.service.RequestService;
import ru.mirea.project.service.SpotService;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;

public class RequestFormController {
    private static final String ERROR_STYLE = "field-error";
    private static final DateTimeFormatter TIME_FORMAT =
        DateTimeFormatter.ofPattern("H:mm").withResolverStyle(ResolverStyle.STRICT);

    private final RequestService requestService;
    private final UserService userService;
    private final VehicleService vehicleService;
    private final SpotService spotService;

    private List<Vehicle> allVehicles = List.of();
    private List<Control> fields = List.of();
    private Request editedRequest;
    private Request savedRequest;

    @FXML
    private DialogPane dialogPane;
    @FXML
    private ButtonType saveButtonType;
    @FXML
    private ComboBox<User> ownerBox;
    @FXML
    private ComboBox<Vehicle> vehicleBox;
    @FXML
    private ComboBox<Spot> spotBox;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private TextField startTimeField;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private TextField endTimeField;
    @FXML
    private Label errorLabel;

    public RequestFormController(AppContext context) {
        this.requestService = context.getRequestService();
        this.userService = context.getUserService();
        this.vehicleService = context.getVehicleService();
        this.spotService = context.getSpotService();
    }

    @FXML
    private void initialize() {
        fields = List.of(ownerBox, vehicleBox, spotBox, startDatePicker, startTimeField, endDatePicker, endTimeField);

        ownerBox.setConverter(textOf(user -> user.getName() + ", " + user.getPhone()));
        vehicleBox.setConverter(textOf(vehicle ->
            vehicle.getLicensePlate() + ", " + vehicle.getBrand() + " " + vehicle.getModel()));
        spotBox.setConverter(textOf(spot -> "№ " + spot.getSpotNumber() + ", "
            + spot.getSpotType().getTitle() + ", " + spot.getHourlyRate() + " руб/ч"));

        ownerBox.valueProperty().addListener((observable, oldOwner, newOwner) -> showVehiclesOf(newOwner));

        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, this::save);
    }

    public void prepare(Request request) {
        editedRequest = request;
        allVehicles = vehicleService.getAll();
        ownerBox.getItems().setAll(userService.getAll());
        spotBox.getItems().setAll(spotService.getAll());
        if (request == null) {
            return;
        }

        ownerBox.setValue(findById(ownerBox.getItems(), User::getId, request.getUserId()));
        ownerBox.setDisable(true);
        vehicleBox.setValue(findById(vehicleBox.getItems(), Vehicle::getId, request.getVehicleId()));
        spotBox.setValue(findById(spotBox.getItems(), Spot::getId, request.getSpotId()));
        startDatePicker.setValue(request.getStartTime().toLocalDate());
        startTimeField.setText(TIME_FORMAT.format(request.getStartTime()));
        endDatePicker.setValue(request.getEndTime().toLocalDate());
        endTimeField.setText(TIME_FORMAT.format(request.getEndTime()));
    }

    public Optional<Request> getSavedRequest() {
        return Optional.ofNullable(savedRequest);
    }

    private void showVehiclesOf(User owner) {
        List<Vehicle> vehicles = owner == null ? List.of()
            : allVehicles.stream().filter(v -> v.getUserId() == owner.getId()).toList();
        vehicleBox.getItems().setAll(vehicles);
        vehicleBox.setValue(null);
        if (owner == null) {
            vehicleBox.setPromptText("Сначала выберите владельца");
        } else if (vehicles.isEmpty()) {
            vehicleBox.setPromptText("У владельца нет автомобилей");
        } else {
            vehicleBox.setPromptText("Выберите автомобиль");
        }
    }

    private void save(ActionEvent event) {
        clearErrors();
        try {
            User owner = required(ownerBox, "Выберите владельца");
            Vehicle vehicle = required(vehicleBox, "Выберите автомобиль владельца");
            Spot spot = required(spotBox, "Выберите парковочное место");
            LocalDateTime start = readDateTime(startDatePicker, startTimeField, "начала");
            LocalDateTime end = readDateTime(endDatePicker, endTimeField, "окончания");
            checkPeriod(start, end);

            if (editedRequest == null) {
                savedRequest = requestService.create(owner.getId(), vehicle.getId(), spot.getId(), start, end);
            } else {
                savedRequest = requestService.update(editedRequest.getId(), vehicle.getId(), spot.getId(), start, end);
            }
        } catch (BusinessException | EntityNotFoundException e) {
            errorLabel.setText(e.getMessage());
            event.consume();
        } catch (RuntimeException e) {
            Alerts.error(e);
            event.consume();
        }
    }

    private <T> T required(ComboBox<T> box, String message) {
        if (box.getValue() == null) {
            throw fieldError(message, box);
        }
        return box.getValue();
    }

    private LocalDateTime readDateTime(DatePicker datePicker, TextField timeField, String what) {
        if (datePicker.getValue() == null) {
            throw fieldError("Выберите дату " + what, datePicker);
        }
        try {
            LocalTime time = LocalTime.parse(timeField.getText().trim(), TIME_FORMAT);
            return datePicker.getValue().atTime(time);
        } catch (DateTimeParseException e) {
            throw fieldError("Время " + what + " введите в формате чч:мм, например 8:30 или 18:00", timeField);
        }
    }

    private void checkPeriod(LocalDateTime start, LocalDateTime end) {
        try {
            requestService.checkPeriod(start, end);
        } catch (BusinessException e) {
            throw fieldError(e.getMessage(), endDatePicker, endTimeField);
        }
    }

    private BusinessException fieldError(String message, Control... invalidFields) {
        for (Control field : invalidFields) {
            field.getStyleClass().add(ERROR_STYLE);
        }
        return new BusinessException(message);
    }

    private void clearErrors() {
        errorLabel.setText("");
        fields.forEach(field -> field.getStyleClass().remove(ERROR_STYLE));
    }

    private static <T> T findById(List<T> items, Function<T, Long> idOf, long id) {
        return items.stream()
            .filter(item -> idOf.apply(item) == id)
            .findFirst()
            .orElse(null);
    }

    private static <T> StringConverter<T> textOf(Function<T, String> toText) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : toText.apply(value);
            }

            @Override
            public T fromString(String text) {
                return null;
            }
        };
    }
}

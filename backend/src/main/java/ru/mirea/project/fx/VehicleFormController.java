package ru.mirea.project.fx;
import javafx.application.Platform;
import java.util.List;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import ru.mirea.project.AppContext;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;

public class VehicleFormController {
    private static final String ERROR_STYLE = "field-error";

    private final VehicleService vehicleService;
    private final UserService userService;

    private List<Control> fields = List.of();
    private Vehicle editedVehicle;
    private Vehicle savedVehicle;

    @FXML
    private DialogPane dialogPane;
    @FXML
    private ButtonType saveButtonType;
    @FXML
    private ComboBox<User> ownerBox;
    @FXML
    private TextField plateField;
    @FXML
    private TextField brandField;
    @FXML
    private TextField modelField;
    @FXML
    private Label ownerHintLabel;
    @FXML
    private Label errorLabel;

    public VehicleFormController(AppContext context) {
        this.vehicleService = context.getVehicleService();
        this.userService = context.getUserService();
    }

    @FXML
    private void initialize() {
        fields = List.of(ownerBox, plateField, brandField, modelField);
        ownerBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(User user) {
                return user == null ? "" : user.getName() + " (" + user.getPhone() + ")";
            }

            @Override
            public User fromString(String text) {
                return null;
            }
        });

        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, this::save);
    }

    public void prepare(Vehicle vehicle) {
        ownerBox.getItems().setAll(userService.getAll());
        editedVehicle = vehicle;
        if (vehicle == null) {
            return;
        }
        ownerBox.getItems().stream()
            .filter(user -> user.getId() == vehicle.getUserId())
            .findFirst()
            .ifPresent(ownerBox::setValue);
        ownerBox.setDisable(true);
        ownerHintLabel.setVisible(true);
        ownerHintLabel.setManaged(true);
        plateField.setText(vehicle.getLicensePlate());
        brandField.setText(vehicle.getBrand());
        modelField.setText(vehicle.getModel());
    }

    public Optional<Vehicle> getSavedVehicle() {
        return Optional.ofNullable(savedVehicle);
    }

    private void save(ActionEvent event) {
        clearErrors();
        try {
            User owner = readOwner();
            String plate = readPlate();
            String brand = readBrand();
            String model = readModel();

            if (editedVehicle == null) {
                savedVehicle = vehicleService.create(owner.getId(), plate, brand, model);
            } else {
                savedVehicle = vehicleService.update(editedVehicle.getId(), plate, brand, model);
            }
        } catch (BusinessException | EntityNotFoundException e) {
            showError(e.getMessage());
            event.consume();
        } catch (RuntimeException e) {
            Alerts.error(e);
            event.consume();
        }
    }

    private User readOwner() {
        User owner = ownerBox.getValue();
        if (owner == null) {
            throw fieldError("Выберите владельца автомобиля", ownerBox);
        }
        return owner;
    }

    private String readPlate() {
        try {
            long editedId = editedVehicle == null ? 0 : editedVehicle.getId();
            return vehicleService.checkLicensePlate(editedId, plateField.getText());
        } catch (BusinessException e) {
            throw fieldError(e.getMessage(), plateField);
        }
    }

    private String readBrand() {
        try {
            return vehicleService.checkBrand(brandField.getText());
        } catch (BusinessException e) {
            throw fieldError(e.getMessage(), brandField);
        }
    }

    private String readModel() {
        try {
            return vehicleService.checkModel(modelField.getText());
        } catch (BusinessException e) {
            throw fieldError(e.getMessage(), modelField);
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
    
    private void showError(String message) {
        errorLabel.setText(message);
        Platform.runLater(() -> dialogPane.getScene().getWindow().sizeToScene());
    }
}
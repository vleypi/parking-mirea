package ru.mirea.project.fx;

import java.math.BigDecimal;
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

import ru.mirea.project.AppContext;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.enums.SpotType;
import ru.mirea.project.service.SpotService;

public class SpotFormController {
    private static final String ERROR_STYLE = "field-error";

    private final SpotService spotService;

    private List<Control> fields = List.of();
    private Spot editedSpot;
    private Spot savedSpot;

    @FXML
    private DialogPane dialogPane;
    @FXML
    private ButtonType saveButtonType;
    @FXML
    private TextField numberField;
    @FXML
    private ComboBox<SpotType> typeBox;
    @FXML
    private TextField rateField;
    @FXML
    private Label errorLabel;

    public SpotFormController(AppContext context) {
        this.spotService = context.getSpotService();
    }

    @FXML
    private void initialize() {
        fields = List.of(numberField, typeBox, rateField);
        typeBox.getItems().setAll(SpotType.values());

        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, this::save);
    }

    public void prepare(Spot spot) {
        editedSpot = spot;
        if (spot == null) {
            return;
        }
        numberField.setText(String.valueOf(spot.getSpotNumber()));
        typeBox.setValue(spot.getSpotType());
        rateField.setText(spot.getHourlyRate().toPlainString());
    }

    public Optional<Spot> getSavedSpot() {
        return Optional.ofNullable(savedSpot);
    }

    private void save(ActionEvent event) {
        clearErrors();
        try {
            int number = readNumber();
            SpotType type = readType();
            BigDecimal rate = readRate();

            if (editedSpot == null) {
                savedSpot = spotService.create(number, type, rate);
            } else {
                savedSpot = spotService.update(editedSpot.getId(), number, type, rate);
            }
        } catch (BusinessException | EntityNotFoundException e) {
            errorLabel.setText(e.getMessage());
            event.consume();
        } catch (RuntimeException e) {
            Alerts.error(e);
            event.consume();
        }
    }

    private int readNumber() {
        try {
            int number = spotService.parseSpotNumber(numberField.getText());
            long editedId = editedSpot == null ? 0 : editedSpot.getId();
            return spotService.checkSpotNumberUnique(editedId, number);
        } catch (BusinessException e) {
            throw fieldError(e.getMessage(), numberField);
        }
    }

    private SpotType readType() {
        try {
            SpotType type = typeBox.getValue();
            spotService.checkSpotType(type);
            return type;
        } catch (BusinessException e) {
            throw fieldError(e.getMessage(), typeBox);
        }
    }

    private BigDecimal readRate() {
        try {
            return spotService.parseHourlyRate(rateField.getText());
        } catch (BusinessException e) {
            throw fieldError(e.getMessage(), rateField);
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
}

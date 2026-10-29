package ru.mirea.project.fx;

import java.util.List;
import java.util.Optional;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import ru.mirea.project.AppContext;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.service.UserService;

/**
 * Форма добавления и редактирования владельца (UserFormDialog.fxml).
 * Все проверки делает UserService (checkName, checkPhone), SQL здесь нет.
 */
public class UserFormController {
    private static final String ERROR_STYLE = "field-error";

    private final UserService userService;

    private List<Control> fields = List.of();
    private User editedUser;
    private User savedUser;

    @FXML
    private DialogPane dialogPane;
    @FXML
    private ButtonType saveButtonType;
    @FXML
    private TextField nameField;
    @FXML
    private TextField phoneField;
    @FXML
    private Label errorLabel;

    public UserFormController(AppContext context) {
        this.userService = context.getUserService();
    }

    @FXML
    private void initialize() {
        fields = List.of(nameField, phoneField);
        // Перехватываем нажатие «Сохранить»: если проверка не прошла, event.consume() не даст окну закрыться
        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, this::save);
    }

    /** null — создание нового владельца, иначе форма открывается заполненной для редактирования. */
    public void prepare(User user) {
        editedUser = user;
        if (user == null) {
            return;
        }
        nameField.setText(user.getName());
        phoneField.setText(user.getPhone());
    }

    public Optional<User> getSavedUser() {
        return Optional.ofNullable(savedUser);
    }

    private void save(ActionEvent event) {
        clearErrors();
        try {
            String name = readName();
            String phone = readPhone();

            if (editedUser == null) {
                savedUser = userService.create(name, phone);
            } else {
                savedUser = userService.update(editedUser.getId(), name, phone);
            }
        } catch (BusinessException | EntityNotFoundException e) {
            showError(e.getMessage());
            event.consume();
        } catch (RuntimeException e) {
            Alerts.error(e);
            event.consume();
        }
    }

    private String readName() {
        try {
            return userService.checkName(nameField.getText());
        } catch (BusinessException e) {
            throw fieldError("Имя: " + e.getMessage(), nameField);
        }
    }

    // checkPhone проверяет: поле заполнено, формат телефона, телефон не занят другим владельцем
    private String readPhone() {
        try {
            long editedId = editedUser == null ? 0 : editedUser.getId();
            return userService.checkPhone(editedId, phoneField.getText());
        } catch (BusinessException e) {
            throw fieldError("Телефон: " + e.getMessage(), phoneField);
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

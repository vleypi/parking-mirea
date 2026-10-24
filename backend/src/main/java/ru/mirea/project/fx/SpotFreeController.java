package ru.mirea.project.fx;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import ru.mirea.project.AppContext;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.service.SpotService;

public class SpotFreeController {
    private final SpotService spotService;

    private final ObservableList<Spot> freeSpots = FXCollections.observableArrayList();

    @FXML
    private DatePicker fromPicker;
    @FXML
    private DatePicker toPicker;
    @FXML
    private Label errorLabel;
    @FXML
    private Label resultLabel;
    @FXML
    private Label placeholderLabel;
    @FXML
    private TableView<Spot> freeTable;
    @FXML
    private TableColumn<Spot, Integer> numberColumn;
    @FXML
    private TableColumn<Spot, String> typeColumn;
    @FXML
    private TableColumn<Spot, BigDecimal> rateColumn;

    public SpotFreeController(AppContext context) {
        this.spotService = context.getSpotService();
    }

    @FXML
    private void initialize() {
        numberColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getSpotNumber()));
        typeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSpotType().getTitle()));
        rateColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getHourlyRate()));
        rateColumn.setCellFactory(column -> new RateCell());
        freeTable.setItems(freeSpots);

        errorLabel.managedProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.visibleProperty().bind(errorLabel.textProperty().isNotEmpty());
    }

    @FXML
    private void search() {
        errorLabel.setText("");
        try {
            Period period = readPeriod();
            freeSpots.setAll(spotService.findFreeDuring(period).stream()
                .sorted(Comparator.comparingInt(Spot::getSpotNumber))
                .toList());
            resultLabel.setText("Свободно мест: " + freeSpots.size() + " (" + period + ")");
            placeholderLabel.setText("На этот период свободных мест нет");
        } catch (BusinessException e) {
            freeSpots.clear();
            resultLabel.setText("");
            errorLabel.setText(e.getMessage());
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
    }

    private Period readPeriod() {
        LocalDate fromDate = fromPicker.getValue();
        LocalDate toDate = toPicker.getValue();
        LocalDateTime from = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime to = toDate == null ? null : toDate.atTime(LocalTime.MAX);
        return new Period(from, to);
    }

    private static class RateCell extends TableCell<Spot, BigDecimal> {
        @Override
        protected void updateItem(BigDecimal value, boolean empty) {
            super.updateItem(value, empty);
            setText(empty || value == null ? null : value.toPlainString());
        }
    }
}

package ru.mirea.project.fx;

import java.math.BigDecimal;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import ru.mirea.project.AppContext;
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.service.SpotService;

public class SpotController {
    private final AppContext context;
    private final SpotService spotService;

    private final ObservableList<Spot> spots = FXCollections.observableArrayList();

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
    private Button editButton;

    public SpotController(AppContext context) {
        this.context = context;
        this.spotService = context.getSpotService();
    }

    @FXML
    private void initialize() {
        numberColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getSpotNumber()));
        typeColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getSpotType().getTitle()));
        rateColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getHourlyRate()));
        rateColumn.setCellFactory(column -> new RateCell());

        spotTable.setItems(spots);
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
        refresh();
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
    private void refresh() {
        try {
            spots.setAll(spotService.getAll());
        } catch (RuntimeException e) {
            Alerts.error(e);
        }
        countLabel.setText("Всего мест: " + spots.size());
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
        spots.stream()
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
}

package ru.mirea.project.fx;

import java.time.format.DateTimeFormatter;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import ru.mirea.project.AppContext;
import ru.mirea.project.model.entity.Request;
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.entity.User;
import ru.mirea.project.model.entity.Vehicle;
import ru.mirea.project.service.SpotService;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;

public class RequestDetailsController {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final UserService userService;
    private final VehicleService vehicleService;
    private final SpotService spotService;

    @FXML
    private Label statusValue;
    @FXML
    private Label periodValue;
    @FXML
    private Label durationValue;
    @FXML
    private Label costValue;
    @FXML
    private Label createdValue;
    @FXML
    private Label ownerNameValue;
    @FXML
    private Label ownerPhoneValue;
    @FXML
    private Label plateValue;
    @FXML
    private Label carValue;
    @FXML
    private Label regionValue;
    @FXML
    private Label spotNumberValue;
    @FXML
    private Label spotTypeValue;
    @FXML
    private Label rateValue;

    public RequestDetailsController(AppContext context) {
        this.userService = context.getUserService();
        this.vehicleService = context.getVehicleService();
        this.spotService = context.getSpotService();
    }

    public void prepare(Request request) {
        User owner = userService.getById(request.getUserId());
        Vehicle vehicle = vehicleService.getById(request.getVehicleId());
        Spot spot = spotService.getById(request.getSpotId());

        statusValue.setText(request.getStatus().getTitle());
        periodValue.setText("с " + DATE_TIME_FORMAT.format(request.getStartTime())
            + " по " + DATE_TIME_FORMAT.format(request.getEndTime()));
        long minutes = request.durationMinutes();
        durationValue.setText(minutes / 60 + " ч " + minutes % 60 + " мин");
        costValue.setText(request.cost(spot.getHourlyRate()).toPlainString() + " руб");
        createdValue.setText(DATE_TIME_FORMAT.format(request.getCreatedAt()));

        ownerNameValue.setText(owner.getName());
        ownerPhoneValue.setText(owner.getPhone());

        plateValue.setText(vehicle.getLicensePlate());
        carValue.setText(vehicle.getBrand() + " " + vehicle.getModel());
        regionValue.setText(vehicle.getRegion());

        spotNumberValue.setText(String.valueOf(spot.getSpotNumber()));
        spotTypeValue.setText(spot.getSpotType().getTitle());
        rateValue.setText(spot.getHourlyRate().toPlainString() + " руб/ч");
    }
}

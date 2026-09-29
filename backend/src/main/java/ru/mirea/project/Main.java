package ru.mirea.project;

import ru.mirea.project.repository.ParkingRequestRepository;
import ru.mirea.project.repository.UserRepository;
import ru.mirea.project.repository.VehicleRepository;
import ru.mirea.project.service.ParkingRequestService;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;
import ru.mirea.project.ui.ConsoleUI;

public class Main {
    public static void main(String[] args) {
        UserRepository userRepository = new UserRepository();
        ParkingRequestRepository parkingRequestRepository = new ParkingRequestRepository();
        VehicleRepository vehicleRepository = new VehicleRepository();
        UserService userService = new UserService(userRepository);
        VehicleService vehicleService = new VehicleService(vehicleRepository, userService);
        ParkingRequestService parkingRequestService = new ParkingRequestService(parkingRequestRepository, userService);
        new ConsoleUI(userService, vehicleService, parkingRequestService).run();
    }
}

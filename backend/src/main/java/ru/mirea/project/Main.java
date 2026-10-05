package ru.mirea.project;

import java.util.List;

import ru.mirea.project.repository.ParkingRequestRepository;
import ru.mirea.project.repository.ParkingSpotRepository;
import ru.mirea.project.repository.UserRepository;
import ru.mirea.project.repository.VehicleRepository;
import ru.mirea.project.service.ParkingRequestService;
import ru.mirea.project.service.ParkingSpotService;
import ru.mirea.project.service.StatisticsService;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;
import ru.mirea.project.ui.ConsoleInput;
import ru.mirea.project.ui.EntityMenu;
import ru.mirea.project.ui.MainMenu;
import ru.mirea.project.ui.ParkingRequestMenu;
import ru.mirea.project.ui.ParkingSpotMenu;
import ru.mirea.project.ui.UserMenu;
import ru.mirea.project.ui.VehicleMenu;

public class Main {
    public static void main(String[] args) {
        UserRepository userRepository = new UserRepository();
        VehicleRepository vehicleRepository = new VehicleRepository();
        ParkingSpotRepository parkingSpotRepository = new ParkingSpotRepository();
        ParkingRequestRepository parkingRequestRepository = new ParkingRequestRepository();

        UserService userService = new UserService(userRepository, vehicleRepository,
            parkingRequestRepository, parkingSpotRepository);
        VehicleService vehicleService = new VehicleService(vehicleRepository, userService,
            parkingRequestRepository, parkingSpotRepository);
        ParkingSpotService parkingSpotService = new ParkingSpotService(parkingSpotRepository, parkingRequestRepository);
        ParkingRequestService parkingRequestService = new ParkingRequestService(parkingRequestRepository,
            userService, vehicleService, parkingSpotService);

        StatisticsService statisticsService = new StatisticsService(userRepository, vehicleRepository,
            parkingSpotRepository, parkingRequestRepository);

        ConsoleInput input = new ConsoleInput();
        List<EntityMenu> menus = List.of(
            new UserMenu(input, userService, vehicleService),
            new VehicleMenu(input, vehicleService, userService),
            new ParkingSpotMenu(input, parkingSpotService),
            new ParkingRequestMenu(input, parkingRequestService, userService, vehicleService, parkingSpotService));
        new MainMenu(input, menus, statisticsService, userService, vehicleService,
            parkingSpotService, parkingRequestService).run();
    }
}

package ru.mirea.project;

import java.util.List;

import ru.mirea.project.repository.RequestRepository;
import ru.mirea.project.repository.SpotRepository;
import ru.mirea.project.repository.UserRepository;
import ru.mirea.project.repository.VehicleRepository;
import ru.mirea.project.service.RequestService;
import ru.mirea.project.service.SpotService;
import ru.mirea.project.service.StatisticsService;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;
import ru.mirea.project.ui.ConsoleInput;
import ru.mirea.project.ui.EntityMenu;
import ru.mirea.project.ui.MainMenu;
import ru.mirea.project.ui.RequestMenu;
import ru.mirea.project.ui.SpotMenu;
import ru.mirea.project.ui.UserMenu;
import ru.mirea.project.ui.VehicleMenu;

public class Main {
    public static void main(String[] args) {
        UserRepository userRepository = new UserRepository();
        VehicleRepository vehicleRepository = new VehicleRepository();
        SpotRepository spotRepository = new SpotRepository();
        RequestRepository requestRepository = new RequestRepository();

        UserService userService = new UserService(userRepository, vehicleRepository,
            requestRepository, spotRepository);
        VehicleService vehicleService = new VehicleService(vehicleRepository, userService,
            requestRepository, spotRepository);
        SpotService spotService = new SpotService(spotRepository, requestRepository);
        RequestService requestService = new RequestService(requestRepository,
            userService, vehicleService, spotService);

        StatisticsService statisticsService = new StatisticsService(userRepository, vehicleRepository,
            spotRepository, requestRepository);

        ConsoleInput input = new ConsoleInput();
        List<EntityMenu> menus = List.of(
            new UserMenu(input, userService, vehicleService),
            new VehicleMenu(input, vehicleService, userService),
            new SpotMenu(input, spotService),
            new RequestMenu(input, requestService, userService, vehicleService, spotService));
        new MainMenu(input, menus, statisticsService, userService, vehicleService,
            spotService, requestService).run();
    }
}

package ru.mirea.project;

import ru.mirea.project.repository.RequestRepository;
import ru.mirea.project.repository.SpotRepository;
import ru.mirea.project.repository.UserRepository;
import ru.mirea.project.repository.VehicleRepository;
import ru.mirea.project.service.RequestService;
import ru.mirea.project.service.SpotService;
import ru.mirea.project.service.StatisticsService;
import ru.mirea.project.service.UserService;
import ru.mirea.project.service.VehicleService;

public class AppContext {
    private final UserService userService;
    private final VehicleService vehicleService;
    private final SpotService spotService;
    private final RequestService requestService;
    private final StatisticsService statisticsService;

    public AppContext() {
        UserRepository userRepository = new UserRepository();
        VehicleRepository vehicleRepository = new VehicleRepository();
        SpotRepository spotRepository = new SpotRepository();
        RequestRepository requestRepository = new RequestRepository();

        userService = new UserService(userRepository, vehicleRepository, requestRepository, spotRepository);
        vehicleService = new VehicleService(vehicleRepository, userService, requestRepository, spotRepository);
        spotService = new SpotService(spotRepository, requestRepository);
        requestService = new RequestService(requestRepository, userService, vehicleService, spotService);
        statisticsService = new StatisticsService(userRepository, vehicleRepository, spotRepository, requestRepository);
    }

    public UserService getUserService() {
        return userService;
    }

    public VehicleService getVehicleService() {
        return vehicleService;
    }

    public SpotService getSpotService() {
        return spotService;
    }

    public RequestService getRequestService() {
        return requestService;
    }

    public StatisticsService getStatisticsService() {
        return statisticsService;
    }
}

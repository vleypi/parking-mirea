package ru.mirea.project.service;

import java.time.LocalDateTime;
import java.util.List;

import ru.mirea.project.dto.GeneralStatistics;
import ru.mirea.project.model.entity.Request;
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.repository.RequestRepository;
import ru.mirea.project.repository.SpotRepository;
import ru.mirea.project.repository.UserRepository;
import ru.mirea.project.repository.VehicleRepository;

public class StatisticsService {
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final SpotRepository spotRepository;
    private final RequestRepository requestRepository;

    public StatisticsService(UserRepository userRepository, VehicleRepository vehicleRepository,
                             SpotRepository spotRepository, RequestRepository requestRepository) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.spotRepository = spotRepository;
        this.requestRepository = requestRepository;
    }

    public GeneralStatistics getGeneralStatistics(Period period) {
        List<Request> allRequests = requestRepository.findAll();
        List<Spot> spots = spotRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        long occupiedNow = allRequests.stream()
            .filter(Request::isActive)
            .filter(r -> !r.getStartTime().isAfter(now) && r.getEndTime().isAfter(now))
            .map(Request::getSpotId)
            .distinct()
            .count();

        List<Request> requests = allRequests.stream()
            .filter(r -> r.overlaps(period))
            .toList();

        return new GeneralStatistics(
            period,
            userRepository.findAll().size(),
            vehicleRepository.findAll().size(),
            spots.size(),
            requests.size(),
            requests.stream().filter(Request::isActive).count(),
            requests.stream().filter(Request::isCompleted).count(),
            requests.stream().filter(Request::isCancelled).count(),
            occupiedNow,
            Request.completedRevenue(requests, Spot.ratesById(spots)));
    }
}

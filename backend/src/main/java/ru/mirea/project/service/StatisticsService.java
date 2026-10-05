package ru.mirea.project.service;

import java.time.LocalDateTime;
import java.util.List;

import ru.mirea.project.dto.GeneralStatistics;
import ru.mirea.project.model.entity.ParkingRequest;
import ru.mirea.project.model.entity.ParkingSpot;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.repository.ParkingRequestRepository;
import ru.mirea.project.repository.ParkingSpotRepository;
import ru.mirea.project.repository.UserRepository;
import ru.mirea.project.repository.VehicleRepository;

public class StatisticsService {
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingRequestRepository parkingRequestRepository;

    public StatisticsService(UserRepository userRepository, VehicleRepository vehicleRepository,
                             ParkingSpotRepository parkingSpotRepository, ParkingRequestRepository parkingRequestRepository) {
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.parkingSpotRepository = parkingSpotRepository;
        this.parkingRequestRepository = parkingRequestRepository;
    }

    public GeneralStatistics getGeneralStatistics(Period period) {
        List<ParkingRequest> allRequests = parkingRequestRepository.findAll();
        List<ParkingSpot> spots = parkingSpotRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        long occupiedNow = allRequests.stream()
            .filter(ParkingRequest::isActive)
            .filter(r -> !r.getStartTime().isAfter(now) && r.getEndTime().isAfter(now))
            .map(ParkingRequest::getSpotId)
            .distinct()
            .count();

        List<ParkingRequest> requests = allRequests.stream()
            .filter(r -> r.overlaps(period))
            .toList();

        return new GeneralStatistics(
            period,
            userRepository.findAll().size(),
            vehicleRepository.findAll().size(),
            spots.size(),
            requests.size(),
            requests.stream().filter(ParkingRequest::isActive).count(),
            requests.stream().filter(ParkingRequest::isCompleted).count(),
            requests.stream().filter(ParkingRequest::isCancelled).count(),
            occupiedNow,
            ParkingRequest.completedRevenue(requests, ParkingSpot.ratesById(spots)));
    }
}

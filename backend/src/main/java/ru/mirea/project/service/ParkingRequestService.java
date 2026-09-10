package ru.mirea.project.service;

import java.time.LocalDateTime;
import java.util.List;

import ru.mirea.project.model.ParkingRequest;
import ru.mirea.project.model.RequestStatus;
import ru.mirea.project.repository.ParkingRequestRepository;

public class ParkingRequestService {
    private final ParkingRequestRepository parkingRequestRepository;
    private final UserService userService;

    public ParkingRequestService(ParkingRequestRepository parkingRequestRepository, UserService userService) {
        this.parkingRequestRepository = parkingRequestRepository;
        this.userService = userService;
    }

    public ParkingRequest create(long userId, String licensePlate, int spotNumber,
                                 LocalDateTime startTime, LocalDateTime endTime) {
        throw new UnsupportedOperationException("Не реализовано");
    }

    public List<ParkingRequest> getAll() {
        throw new UnsupportedOperationException("Не реализовано");
    }

    public ParkingRequest getById(long id) {
        throw new UnsupportedOperationException("Не реализовано");
    }

    public ParkingRequest update(long id, String licensePlate, int spotNumber,
                                 LocalDateTime startTime, LocalDateTime endTime) {
        throw new UnsupportedOperationException("Не реализовано");
    }

    public ParkingRequest changeStatus(long id, RequestStatus newStatus) {
        throw new UnsupportedOperationException("Не реализовано");
    }

    public void delete(long id) {
        throw new UnsupportedOperationException("Не реализовано");
    }
}

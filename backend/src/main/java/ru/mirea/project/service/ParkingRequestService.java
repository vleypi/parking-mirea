package ru.mirea.project.service;

import java.time.LocalDateTime;
import java.util.List;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
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
        validatePlateAndTimes(licensePlate, startTime, endTime);
        userService.getById(userId);

        ParkingRequest request = new ParkingRequest(0, userId, licensePlate, spotNumber,
            startTime, endTime, RequestStatus.NEW, LocalDateTime.now());
        return parkingRequestRepository.create(request);
    }

    public List<ParkingRequest> getAll() {
        throw new UnsupportedOperationException("Не реализовано");
    }

    public ParkingRequest getById(long id) {
        return parkingRequestRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Заявка с id " + id + " не найдена"));
    }

    public ParkingRequest update(long id, String licensePlate, int spotNumber,
                                 LocalDateTime startTime, LocalDateTime endTime) {
        validatePlateAndTimes(licensePlate, startTime, endTime);

        ParkingRequest existing = getById(id);
        existing.setLicensePlate(licensePlate);
        existing.setSpotNumber(spotNumber);
        existing.setStartTime(startTime);
        existing.setEndTime(endTime);
        parkingRequestRepository.update(existing);
        return existing;
    }

    public ParkingRequest changeStatus(long id, RequestStatus newStatus) {
        throw new UnsupportedOperationException("Не реализовано");
    }

    public void delete(long id) {
        throw new UnsupportedOperationException("Не реализовано");
    }

    private void validatePlateAndTimes(String licensePlate, LocalDateTime startTime, LocalDateTime endTime) {
        if (licensePlate == null || licensePlate.isBlank()) {
            throw new BusinessException("Гос. номер обязателен для заполнения");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("Дата и время окончания должны быть позже даты и времени начала");
        }
    }
}

package ru.mirea.project.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.ParkingSpot;
import ru.mirea.project.model.SpotType;
import ru.mirea.project.repository.ParkingSpotRepository;

public class ParkingSpotService {
    private static final BigDecimal MAX_HOURLY_RATE = new BigDecimal("99999.99");

    private final ParkingSpotRepository parkingSpotRepository;

    public ParkingSpotService(ParkingSpotRepository parkingSpotRepository) {
        this.parkingSpotRepository = parkingSpotRepository;
    }

    public ParkingSpot create(int spotNumber, SpotType spotType, BigDecimal hourlyRate) {
        checkSpotNumber(spotNumber);
        checkSpotType(spotType);
        BigDecimal rate = checkHourlyRate(hourlyRate);
        checkSpotNumberUnique(0, spotNumber);
        ParkingSpot spot = new ParkingSpot(0, spotNumber, spotType, rate);
        return parkingSpotRepository.create(spot);
    }

    public List<ParkingSpot> getAll() {
        return parkingSpotRepository.findAll();
    }

    public ParkingSpot getById(long id) {
        return parkingSpotRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Парковочное место с id " + id + " не найдено"));
    }

    public List<ParkingSpot> filterByType(SpotType spotType) {
        return parkingSpotRepository.findAll().stream()
            .filter(s -> s.getSpotType() == spotType)
            .toList();
    }

    public ParkingSpot update(long id, int spotNumber, SpotType spotType, BigDecimal hourlyRate) {
        ParkingSpot existing = getById(id);
        checkSpotNumber(spotNumber);
        checkSpotType(spotType);
        BigDecimal rate = checkHourlyRate(hourlyRate);
        checkSpotNumberUnique(id, spotNumber);

        existing.setSpotNumber(spotNumber);
        existing.setSpotType(spotType);
        existing.setHourlyRate(rate);
        parkingSpotRepository.update(existing);
        return existing;
    }

    public void delete(long id) {
        getById(id);
        parkingSpotRepository.delete(id);
    }

    public int parseSpotNumber(String raw) {
        int spotNumber;
        try {
            spotNumber = Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("Номер места должен быть целым числом");
        }
        checkSpotNumber(spotNumber);
        return spotNumber;
    }

    public BigDecimal parseHourlyRate(String raw) {
        BigDecimal rate;
        try {
            rate = new BigDecimal(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new BusinessException("Тариф должен быть числом, например 100 или 99.50");
        }
        return checkHourlyRate(rate);
    }

    public void checkSpotNumber(int spotNumber) {
        if (spotNumber <= 0) {
            throw new BusinessException("Номер места должен быть положительным числом");
        }
    }

    public void checkSpotType(SpotType spotType) {
        if (spotType == null) {
            throw new BusinessException("Тип места обязателен для заполнения");
        }
    }

    public BigDecimal checkHourlyRate(BigDecimal rate) {
        if (rate == null) {
            throw new BusinessException("Тариф обязателен для заполнения");
        }
        if (rate.signum() < 0) {
            throw new BusinessException("Тариф не может быть отрицательным");
        }
        BigDecimal rounded = rate.setScale(2, RoundingMode.HALF_UP);
        if (rounded.compareTo(MAX_HOURLY_RATE) > 0) {
            throw new BusinessException("Тариф не должен превышать " + MAX_HOURLY_RATE);
        }
        return rounded;
    }

    public int checkSpotNumberUnique(long spotId, int spotNumber) {
        boolean taken = parkingSpotRepository.findAll().stream()
            .filter(s -> s.getId() != spotId)
            .anyMatch(s -> s.getSpotNumber() == spotNumber);

        if (taken) {
            throw new BusinessException("Место с номером " + spotNumber + " уже существует");
        }
        return spotNumber;
    }
}

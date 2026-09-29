package ru.mirea.project.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
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
        checkSpotNumberUnique(0, spotNumber);
        ParkingSpot spot = new ParkingSpot(0, spotNumber, spotType, hourlyRate);
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
        checkSpotNumberUnique(id, spotNumber);

        existing.setSpotNumber(spotNumber);
        existing.setSpotType(spotType);
        existing.setHourlyRate(hourlyRate);
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
        if (spotNumber <= 0) {
            throw new BusinessException("Номер места должен быть положительным числом");
        }
        return spotNumber;
    }

    public SpotType parseSpotType(String raw) {
        try {
            return SpotType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Неизвестный тип места. Допустимые значения: " + Arrays.toString(SpotType.values()));
        }
    }

    public BigDecimal parseHourlyRate(String raw) {
        BigDecimal rate;
        try {
            rate = new BigDecimal(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new BusinessException("Тариф должен быть числом, например 100 или 99.50");
        }
        if (rate.signum() < 0) {
            throw new BusinessException("Тариф не может быть отрицательным");
        }
        if (rate.compareTo(MAX_HOURLY_RATE) > 0) {
            throw new BusinessException("Тариф не должен превышать " + MAX_HOURLY_RATE);
        }
        return rate.setScale(2, RoundingMode.HALF_UP);
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

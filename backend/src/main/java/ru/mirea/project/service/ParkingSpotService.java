package ru.mirea.project.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ru.mirea.project.dto.ParkingSpotStatistics;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.entity.ParkingRequest;
import ru.mirea.project.model.entity.ParkingSpot;
import ru.mirea.project.model.enums.SpotType;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.repository.ParkingRequestRepository;
import ru.mirea.project.repository.ParkingSpotRepository;

public class ParkingSpotService {
    private static final BigDecimal MAX_HOURLY_RATE = new BigDecimal("99999.99");

    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingRequestRepository parkingRequestRepository;

    public ParkingSpotService(ParkingSpotRepository parkingSpotRepository, ParkingRequestRepository parkingRequestRepository) {
        this.parkingSpotRepository = parkingSpotRepository;
        this.parkingRequestRepository = parkingRequestRepository;
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

    public ParkingSpot findByNumber(int spotNumber) {
        return parkingSpotRepository.findAll().stream()
            .filter(s -> s.getSpotNumber() == spotNumber)
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Место с номером " + spotNumber + " не найдено"));
    }

    public List<ParkingSpot> findFreeDuring(Period period) {
        if (!period.isBounded()) {
            throw new BusinessException("Укажите начало и конец периода");
        }
        Set<Long> busySpotIds = parkingRequestRepository.findAll().stream()
            .filter(ParkingRequest::isActive)
            .filter(r -> r.overlaps(period))
            .map(ParkingRequest::getSpotId)
            .collect(Collectors.toSet());
        return parkingSpotRepository.findAll().stream()
            .filter(s -> !busySpotIds.contains(s.getId()))
            .toList();
    }

    public List<ParkingSpot> filterByRateRange(BigDecimal min, BigDecimal max) {
        if (min == null || max == null) {
            throw new BusinessException("Укажите обе границы тарифа");
        }
        if (min.compareTo(max) > 0) {
            throw new BusinessException("Нижняя граница тарифа не может быть больше верхней");
        }
        return parkingSpotRepository.findAll().stream()
            .filter(s -> s.getHourlyRate().compareTo(min) >= 0 && s.getHourlyRate().compareTo(max) <= 0)
            .toList();
    }

    public List<ParkingSpot> sortByNumber(boolean ascending) {
        return sorted(Comparator.comparingInt(ParkingSpot::getSpotNumber), ascending);
    }

    public List<ParkingSpot> sortByRate(boolean ascending) {
        return sorted(Comparator.comparing(ParkingSpot::getHourlyRate)
            .thenComparingInt(ParkingSpot::getSpotNumber), ascending);
    }

    public ParkingSpotStatistics getStatistics(long spotId, Period period) {
        ParkingSpot spot = getById(spotId);
        List<ParkingRequest> requests = parkingRequestRepository.findAll().stream()
            .filter(r -> r.getSpotId() == spotId)
            .filter(r -> !r.isCancelled())
            .filter(r -> r.overlaps(period))
            .toList();
        long minutes = requests.stream().mapToLong(ParkingRequest::durationMinutes).sum();
        BigDecimal revenue = requests.stream()
            .filter(ParkingRequest::isCompleted)
            .map(r -> r.cost(spot.getHourlyRate()))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        return new ParkingSpotStatistics(spot, period, requests.size(), ParkingRequest.hours(minutes), revenue);
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

    private List<ParkingSpot> sorted(Comparator<ParkingSpot> comparator, boolean ascending) {
        return parkingSpotRepository.findAll().stream()
            .sorted(ascending ? comparator : comparator.reversed())
            .toList();
    }
}

package ru.mirea.project.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.mirea.project.dto.filter.SpotFilter;
import ru.mirea.project.dto.statistics.SpotStatistics;
import ru.mirea.project.exception.BusinessException;
import ru.mirea.project.exception.EntityNotFoundException;
import ru.mirea.project.model.entity.Request;
import ru.mirea.project.model.entity.Spot;
import ru.mirea.project.model.enums.SpotType;
import ru.mirea.project.model.value.NumberRange;
import ru.mirea.project.model.value.Period;
import ru.mirea.project.repository.RequestRepository;
import ru.mirea.project.repository.SpotRepository;

public class SpotService {
    private static final BigDecimal MAX_HOURLY_RATE = new BigDecimal("99999.99");

    private final SpotRepository spotRepository;
    private final RequestRepository requestRepository;

    public SpotService(SpotRepository spotRepository, RequestRepository requestRepository) {
        this.spotRepository = spotRepository;
        this.requestRepository = requestRepository;
    }

    public Spot create(int spotNumber, SpotType spotType, BigDecimal hourlyRate) {
        checkSpotNumber(spotNumber);
        checkSpotType(spotType);
        BigDecimal rate = checkHourlyRate(hourlyRate);
        checkSpotNumberUnique(0, spotNumber);
        Spot spot = new Spot(0, spotNumber, spotType, rate);
        return spotRepository.create(spot);
    }

    public List<Spot> getAll() {
        return spotRepository.findAll();
    }

    public Spot getById(long id) {
        return spotRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Парковочное место с id " + id + " не найдено"));
    }

    public Spot findByNumber(int spotNumber) {
        return spotRepository.findAll().stream()
            .filter(s -> s.getSpotNumber() == spotNumber)
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Место с номером " + spotNumber + " не найдено"));
    }

    public List<Spot> findFreeDuring(Period period) {
        if (!period.isBounded()) {
            throw new BusinessException("Укажите начало и конец периода");
        }
        Set<Long> busySpotIds = requestRepository.findAll().stream()
            .filter(Request::isActive)
            .filter(r -> r.overlaps(period))
            .map(Request::getSpotId)
            .collect(Collectors.toSet());
        return spotRepository.findAll().stream()
            .filter(s -> !busySpotIds.contains(s.getId()))
            .toList();
    }

    public List<Spot> find(SpotFilter filter) {
        Stream<Spot> spots = spotRepository.findAll().stream();
        if (filter.getNumberRange() != null) {
            NumberRange range = filter.getNumberRange();
            spots = spots.filter(s -> range.contains(s.getSpotNumber()));
        }
        if (filter.getType() != null) {
            SpotType type = filter.getType();
            spots = spots.filter(s -> s.getSpotType() == type);
        }
        if (filter.getRateRange() != null) {
            NumberRange range = filter.getRateRange();
            spots = spots.filter(s -> range.contains(s.getHourlyRate()));
        }

        Comparator<Spot> comparator = switch (filter.getSortField()) {
            case ID -> Comparator.comparingLong(Spot::getId);
            case NUMBER -> Comparator.comparingInt(Spot::getSpotNumber);
            case TYPE -> Comparator.comparingInt((Spot s) -> s.getSpotType().getId())
                .thenComparingInt(Spot::getSpotNumber);
            case RATE -> Comparator.comparing(Spot::getHourlyRate).thenComparingInt(Spot::getSpotNumber);
        };
        return spots.sorted(filter.isAscending() ? comparator : comparator.reversed()).toList();
    }

    public SpotStatistics getStatistics(long spotId, Period period) {
        Spot spot = getById(spotId);
        List<Request> requests = requestRepository.findAll().stream()
            .filter(r -> r.getSpotId() == spotId)
            .filter(r -> !r.isCancelled())
            .filter(r -> r.overlaps(period))
            .toList();
        long minutes = requests.stream().mapToLong(Request::durationMinutes).sum();
        BigDecimal revenue = requests.stream()
            .filter(Request::isCompleted)
            .map(r -> r.cost(spot.getHourlyRate()))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        return new SpotStatistics(spot, period, requests.size(), Request.hours(minutes), revenue);
    }

    public Spot update(long id, int spotNumber, SpotType spotType, BigDecimal hourlyRate) {
        Spot existing = getById(id);
        checkSpotNumber(spotNumber);
        checkSpotType(spotType);
        BigDecimal rate = checkHourlyRate(hourlyRate);
        checkSpotNumberUnique(id, spotNumber);

        existing.setSpotNumber(spotNumber);
        existing.setSpotType(spotType);
        existing.setHourlyRate(rate);
        spotRepository.update(existing);
        return existing;
    }

    public void delete(long id) {
        getById(id);
        spotRepository.delete(id);
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
        boolean taken = spotRepository.findAll().stream()
            .filter(s -> s.getId() != spotId)
            .anyMatch(s -> s.getSpotNumber() == spotNumber);

        if (taken) {
            throw new BusinessException("Место с номером " + spotNumber + " уже существует");
        }
        return spotNumber;
    }
}

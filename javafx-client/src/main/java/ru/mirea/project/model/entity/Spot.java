package ru.mirea.project.model.entity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.mirea.project.model.enums.SpotType;

public class Spot {
    private long id;
    private int spotNumber;
    private SpotType spotType;
    private BigDecimal hourlyRate;

    public Spot(long id, int spotNumber, SpotType spotType, BigDecimal hourlyRate) {
        this.id = id;
        this.spotNumber = spotNumber;
        this.spotType = spotType;
        this.hourlyRate = hourlyRate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getSpotNumber() {
        return spotNumber;
    }

    public void setSpotNumber(int spotNumber) {
        this.spotNumber = spotNumber;
    }

    public SpotType getSpotType() {
        return spotType;
    }

    public void setSpotType(SpotType spotType) {
        this.spotType = spotType;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public static Map<Long, BigDecimal> ratesById(List<Spot> spots) {
        return spots.stream().collect(Collectors.toMap(Spot::getId, Spot::getHourlyRate));
    }

    @Override
    public String toString() {
        return "[%d] место %d | %s | %s руб/ч".formatted(id, spotNumber, spotType, hourlyRate);
    }
}

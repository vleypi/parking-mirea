package ru.mirea.project.model;

import java.math.BigDecimal;

public class ParkingSpot {
    private long id;
    private int spotNumber;
    private SpotType spotType;
    private BigDecimal hourlyRate;

    public ParkingSpot(long id, int spotNumber, SpotType spotType, BigDecimal hourlyRate) {
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

    @Override
    public String toString() {
        return "[%d] место %d | %s | %s руб/ч".formatted(id, spotNumber, spotType, hourlyRate);
    }
}

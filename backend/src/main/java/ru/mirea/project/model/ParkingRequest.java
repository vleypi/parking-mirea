package ru.mirea.project.model;

import java.time.LocalDateTime;

public class ParkingRequest {
    private long id;
    private long userId;
    private String licensePlate;
    private int spotNumber;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private RequestStatus status;
    private LocalDateTime createdAt;

    public ParkingRequest(long id, long userId, String licensePlate, int spotNumber,
                          LocalDateTime startTime, LocalDateTime endTime,
                          RequestStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.licensePlate = licensePlate;
        this.spotNumber = spotNumber;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public int getSpotNumber() {
        return spotNumber;
    }

    public void setSpotNumber(int spotNumber) {
        this.spotNumber = spotNumber;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "[%d] место %d | %s | %s - %s | статус: %s | владелец: %d"
                .formatted(id, spotNumber, licensePlate, startTime, endTime, status, userId);
    }
}

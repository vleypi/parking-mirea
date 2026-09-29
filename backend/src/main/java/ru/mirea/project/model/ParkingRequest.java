package ru.mirea.project.model;

import java.time.LocalDateTime;

public class ParkingRequest {
    private long id;
    private long userId;
    private long vehicleId;
    private long spotId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private RequestStatus status;
    private LocalDateTime createdAt;

    public ParkingRequest(long id, long userId, long vehicleId, long spotId,
                          LocalDateTime startTime, LocalDateTime endTime,
                          RequestStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.vehicleId = vehicleId;
        this.spotId = spotId;
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

    public long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public long getSpotId() {
        return spotId;
    }

    public void setSpotId(long spotId) {
        this.spotId = spotId;
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
        return "[%d] владелец: %d | авто: %d | место: %d | %s - %s | статус: %s"
                .formatted(id, userId, vehicleId, spotId, startTime, endTime, status);
    }
}

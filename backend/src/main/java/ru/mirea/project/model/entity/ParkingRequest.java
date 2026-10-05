package ru.mirea.project.model.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import ru.mirea.project.model.enums.RequestStatus;
import ru.mirea.project.model.value.Period;

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

    public boolean isActive() {
        return status == RequestStatus.NEW || status == RequestStatus.CONFIRMED;
    }

    public boolean isCancelled() {
        return status == RequestStatus.CANCELLED;
    }

    public boolean isCompleted() {
        return status == RequestStatus.COMPLETED;
    }

    public boolean overlaps(LocalDateTime start, LocalDateTime end) {
        return startTime.isBefore(end) && endTime.isAfter(start);
    }

    public boolean overlaps(Period period) {
        return period.overlaps(startTime, endTime);
    }

    public long durationMinutes() {
        return Duration.between(startTime, endTime).toMinutes();
    }

    public BigDecimal cost(BigDecimal hourlyRate) {
        return hourlyRate.multiply(BigDecimal.valueOf(durationMinutes()))
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    public static Map<RequestStatus, Long> countByStatus(List<ParkingRequest> requests) {
        Map<RequestStatus, Long> counts = new EnumMap<>(RequestStatus.class);
        for (RequestStatus status : RequestStatus.values()) {
            counts.put(status, 0L);
        }
        requests.forEach(r -> counts.merge(r.getStatus(), 1L, Long::sum));
        return counts;
    }

    public static BigDecimal completedRevenue(List<ParkingRequest> requests, Map<Long, BigDecimal> ratesBySpotId) {
        return requests.stream()
            .filter(ParkingRequest::isCompleted)
            .map(r -> r.cost(ratesBySpotId.get(r.getSpotId())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal hours(long minutes) {
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
    }

    @Override
    public String toString() {
        return "[%d] владелец: %d | авто: %d | место: %d | %s - %s | статус: %s"
                .formatted(id, userId, vehicleId, spotId, startTime, endTime, status);
    }
}

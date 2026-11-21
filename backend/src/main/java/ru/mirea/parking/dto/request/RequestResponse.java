package ru.mirea.parking.dto.request;

import ru.mirea.parking.domain.Owner;
import ru.mirea.parking.domain.Request;
import ru.mirea.parking.domain.RequestPriority;
import ru.mirea.parking.domain.RequestStatus;
import ru.mirea.parking.domain.Spot;
import ru.mirea.parking.domain.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

public record RequestResponse(
        UUID id,
        String code,
        OwnerSummary owner,
        VehicleSummary vehicle,
        SpotSummary spot,
        StatusSummary status,
        RequestPriority priority,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String description,
        BigDecimal cost,
        boolean paid,
        boolean archived,
        Set<String> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long version) {

    public record OwnerSummary(UUID id, String name, String phone) {
        static OwnerSummary from(Owner owner) {
            return new OwnerSummary(owner.getUuid(), owner.getName(), owner.getPhone());
        }
    }

    public record VehicleSummary(UUID id, String licensePlate, String brand, String model) {
        static VehicleSummary from(Vehicle vehicle) {
            return new VehicleSummary(vehicle.getUuid(), vehicle.getLicensePlate(), vehicle.getBrand(), vehicle.getModel());
        }
    }

    public record SpotSummary(UUID id, Integer number, String type, String typeTitle, BigDecimal hourlyRate) {
        static SpotSummary from(Spot spot) {
            return new SpotSummary(spot.getUuid(), spot.getSpotNumber(), spot.getSpotType().getCode(),
                    spot.getSpotType().getTitle(), spot.getHourlyRate());
        }
    }

    public record StatusSummary(String code, String title) {
        static StatusSummary from(RequestStatus status) {
            return new StatusSummary(status.getCode(), status.getTitle());
        }
    }

    public static RequestResponse from(Request request, RequestPriority priority) {
        return new RequestResponse(
                request.getUuid(),
                request.getCode(),
                OwnerSummary.from(request.getOwner()),
                VehicleSummary.from(request.getVehicle()),
                SpotSummary.from(request.getSpot()),
                StatusSummary.from(request.getStatus()),
                priority,
                request.getStartTime(),
                request.getEndTime(),
                request.getDescription(),
                request.getCost(),
                request.isPaid(),
                request.isArchived(),
                new TreeSet<>(request.getTags()),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.getVersion());
    }
}

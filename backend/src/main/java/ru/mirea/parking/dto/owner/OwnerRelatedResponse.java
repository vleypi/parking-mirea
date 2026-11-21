package ru.mirea.parking.dto.owner;

import ru.mirea.parking.domain.Request;
import ru.mirea.parking.domain.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Связанные записи для карточки владельца: его машины и последние заявки. */
public record OwnerRelatedResponse(List<VehicleItem> vehicles, List<RequestItem> requests, long requestsTotal) {

    public record VehicleItem(UUID id, String licensePlate, String brand, String model) {
        public static VehicleItem from(Vehicle vehicle) {
            return new VehicleItem(vehicle.getUuid(), vehicle.getLicensePlate(), vehicle.getBrand(), vehicle.getModel());
        }
    }

    public record RequestItem(UUID id, String code, String licensePlate, Integer spotNumber, String statusCode,
                              String statusTitle, LocalDateTime startTime, LocalDateTime endTime, BigDecimal cost,
                              boolean archived) {
        public static RequestItem from(Request request) {
            return new RequestItem(request.getUuid(), request.getCode(), request.getVehicle().getLicensePlate(),
                    request.getSpot().getSpotNumber(), request.getStatus().getCode(), request.getStatus().getTitle(),
                    request.getStartTime(), request.getEndTime(), request.getCost(), request.isArchived());
        }
    }
}

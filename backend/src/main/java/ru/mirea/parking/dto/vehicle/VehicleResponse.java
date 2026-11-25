package ru.mirea.parking.dto.vehicle;

import ru.mirea.parking.domain.Owner;
import ru.mirea.parking.domain.Vehicle;
import ru.mirea.parking.domain.VehicleActivity;
import ru.mirea.parking.service.VehicleActivityCalculator;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Автомобиль в ответах API. */
public record VehicleResponse(
        UUID id,
        String licensePlate,
        String region,
        String brand,
        String model,
        String color,
        Short releaseYear,
        boolean electric,
        CodeTitle status,
        OwnerSummary owner,
        long recentRequests,
        ActivitySummary activity,
        boolean archived,
        Set<String> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long version) {

    public record CodeTitle(String code, String title) {
    }

    public record ActivitySummary(VehicleActivity code, String title) {
    }

    public record OwnerSummary(UUID id, String name, String phone, boolean archived) {
        static OwnerSummary from(Owner owner) {
            return new OwnerSummary(owner.getUuid(), owner.getName(), owner.getPhone(), owner.isArchived());
        }
    }

    /** recentRequests — заявки за окно активности, посчитанные сервисом одним запросом на страницу. */
    public static VehicleResponse from(Vehicle vehicle, long recentRequests) {
        VehicleActivity activity = VehicleActivityCalculator.calculate(recentRequests);
        return new VehicleResponse(
                vehicle.getUuid(),
                vehicle.getLicensePlate(),
                vehicle.getRegion(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getColor(),
                vehicle.getReleaseYear(),
                vehicle.isElectric(),
                new CodeTitle(vehicle.getStatus().name(), vehicle.getStatus().getTitle()),
                OwnerSummary.from(vehicle.getOwner()),
                recentRequests,
                new ActivitySummary(activity, activity.getTitle()),
                vehicle.isArchived(),
                new TreeSet<>(vehicle.getTags()),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt(),
                vehicle.getVersion());
    }
}

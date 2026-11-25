package ru.mirea.parking.dto.vehicle;

import ru.mirea.parking.domain.Request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Заявки автомобиля для его карточки: последние записи и общее число. */
public record VehicleRequestsResponse(List<Item> requests, long total) {

    public record Item(UUID id, String code, String ownerName, Integer spotNumber, String statusCode,
                       String statusTitle, LocalDateTime startTime, LocalDateTime endTime, BigDecimal cost,
                       boolean archived) {
        public static Item from(Request request) {
            return new Item(request.getUuid(), request.getCode(), request.getOwner().getName(),
                    request.getSpot().getSpotNumber(), request.getStatus().getCode(), request.getStatus().getTitle(),
                    request.getStartTime(), request.getEndTime(), request.getCost(), request.isArchived());
        }
    }
}

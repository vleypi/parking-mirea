package ru.mirea.parking.dto.spot;

import ru.mirea.parking.domain.Spot;
import ru.mirea.parking.domain.SpotAvailability;
import ru.mirea.parking.domain.SpotType;
import ru.mirea.parking.service.SpotAvailabilityCalculator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Парковочное место в ответах API. */
public record SpotResponse(
        UUID id,
        int spotNumber,
        TypeSummary spotType,
        BigDecimal hourlyRate,
        String zone,
        String description,
        CodeTitle status,
        boolean covered,
        AvailabilitySummary availability,
        boolean archived,
        Set<String> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long version) {

    public record CodeTitle(String code, String title) {
    }

    public record TypeSummary(short id, String code, String title, boolean active) {
        static TypeSummary from(SpotType type) {
            return new TypeSummary(type.getId(), type.getCode(), type.getTitle(), type.isActive());
        }
    }

    public record AvailabilitySummary(SpotAvailability code, String title) {
    }

    /** occupiedNow — есть ли текущая заявка, посчитано сервисом одним запросом на страницу. */
    public static SpotResponse from(Spot spot, boolean occupiedNow) {
        SpotAvailability availability = SpotAvailabilityCalculator.calculate(spot.getStatus(), occupiedNow);
        return new SpotResponse(
                spot.getUuid(),
                spot.getSpotNumber(),
                TypeSummary.from(spot.getSpotType()),
                spot.getHourlyRate(),
                spot.getZone(),
                spot.getDescription(),
                new CodeTitle(spot.getStatus().name(), spot.getStatus().getTitle()),
                spot.isCovered(),
                new AvailabilitySummary(availability, availability.getTitle()),
                spot.isArchived(),
                new TreeSet<>(spot.getTags()),
                spot.getCreatedAt(),
                spot.getUpdatedAt(),
                spot.getVersion());
    }
}

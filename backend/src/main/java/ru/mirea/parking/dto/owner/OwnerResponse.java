package ru.mirea.parking.dto.owner;

import ru.mirea.parking.domain.Owner;
import ru.mirea.parking.dto.reference.ClientCategoryResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Владелец в ответах API. JPA-сущность наружу не отдаётся. */
public record OwnerResponse(
        UUID id,
        String name,
        String phone,
        String email,
        ClientCategoryResponse category,
        StatusSummary status,
        BigDecimal discountPercent,
        boolean notificationsEnabled,
        long vehiclesCount,
        boolean archived,
        Set<String> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long version) {

    public record StatusSummary(String code, String title) {
    }

    public static OwnerResponse from(Owner owner, long vehiclesCount) {
        return new OwnerResponse(
                owner.getUuid(),
                owner.getName(),
                owner.getPhone(),
                owner.getEmail(),
                ClientCategoryResponse.from(owner.getCategory()),
                new StatusSummary(owner.getStatus().name(), owner.getStatus().getTitle()),
                owner.getDiscountPercent(),
                owner.isNotificationsEnabled(),
                vehiclesCount,
                owner.isArchived(),
                new TreeSet<>(owner.getTags()),
                owner.getCreatedAt(),
                owner.getUpdatedAt(),
                owner.getVersion());
    }
}

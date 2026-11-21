package ru.mirea.parking.dto.owner;

import ru.mirea.parking.domain.LoyaltyLevel;
import ru.mirea.parking.domain.Owner;
import ru.mirea.parking.dto.reference.ClientCategoryResponse;
import ru.mirea.parking.service.LoyaltyCalculator;

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
        long completedRequests,
        LoyaltySummary loyalty,
        boolean archived,
        Set<String> tags,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long version) {

    public record StatusSummary(String code, String title) {
    }

    public record LoyaltySummary(LoyaltyLevel code, String title) {
        static LoyaltySummary from(LoyaltyLevel level) {
            return new LoyaltySummary(level, level.getTitle());
        }
    }

    /** Счётчики считаются в сервисе одним GROUP BY на страницу, сюда приходят готовые числа. */
    public record Counters(long vehicles, long completedRequests) {
        public static final Counters ZERO = new Counters(0, 0);
    }

    public static OwnerResponse from(Owner owner, Counters counters) {
        return new OwnerResponse(
                owner.getUuid(),
                owner.getName(),
                owner.getPhone(),
                owner.getEmail(),
                ClientCategoryResponse.from(owner.getCategory()),
                new StatusSummary(owner.getStatus().name(), owner.getStatus().getTitle()),
                owner.getDiscountPercent(),
                owner.isNotificationsEnabled(),
                counters.vehicles(),
                counters.completedRequests(),
                LoyaltySummary.from(LoyaltyCalculator.calculate(counters.completedRequests())),
                owner.isArchived(),
                new TreeSet<>(owner.getTags()),
                owner.getCreatedAt(),
                owner.getUpdatedAt(),
                owner.getVersion());
    }
}

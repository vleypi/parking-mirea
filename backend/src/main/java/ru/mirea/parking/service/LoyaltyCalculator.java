package ru.mirea.parking.service;

import ru.mirea.parking.domain.LoyaltyLevel;

/**
 * Вычисляемый показатель «Лояльность» владельца по количеству его завершённых заявок (статус COMPLETED).
 * Архивные заявки тоже считаются: архив скрывает запись из списка, но не отменяет историю посещений.
 * <p>
 * Это же правило для фильтра GET /api/owners?loyalty=... выражено подзапросом COUNT
 * в {@link ru.mirea.parking.filter.OwnerSpecifications#hasLoyalty}, границы берутся из констант ниже.
 */
public final class LoyaltyCalculator {

    /** С этого количества завершённых заявок владелец REGULAR. */
    public static final long REGULAR_FROM = 3;
    /** С этого количества завершённых заявок владелец VIP. */
    public static final long VIP_FROM = 10;

    private LoyaltyCalculator() {
    }

    public static LoyaltyLevel calculate(long completedRequests) {
        if (completedRequests < 0) {
            throw new IllegalArgumentException("Количество заявок не может быть отрицательным: " + completedRequests);
        }
        if (completedRequests >= VIP_FROM) {
            return LoyaltyLevel.VIP;
        }
        if (completedRequests >= REGULAR_FROM) {
            return LoyaltyLevel.REGULAR;
        }
        return LoyaltyLevel.NEW;
    }
}

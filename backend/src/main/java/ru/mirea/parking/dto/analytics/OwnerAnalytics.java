package ru.mirea.parking.dto.analytics;

import ru.mirea.parking.domain.LoyaltyLevel;
import ru.mirea.parking.domain.OwnerStatus;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Блок «Владельцы» общей сводки.
 *
 * @param total                всего владельцев при заданных условиях
 * @param active               не в архиве
 * @param archived             в архиве
 * @param notificationsEnabled с включёнными уведомлениями
 * @param averageDiscount      средняя скидка, %
 * @param byCategory           по категории клиента (код → количество), все категории справочника, даже с нулём
 * @param byStatus             по статусу ACTIVE / BLOCKED
 * @param byLoyalty            по уровню лояльности NEW / REGULAR / VIP
 */
public record OwnerAnalytics(
        long total,
        long active,
        long archived,
        long notificationsEnabled,
        BigDecimal averageDiscount,
        Map<String, Long> byCategory,
        Map<OwnerStatus, Long> byStatus,
        Map<LoyaltyLevel, Long> byLoyalty) {
}

package ru.mirea.parking.domain;

/**
 * Уровень лояльности владельца. Не хранится в базе, а вычисляется по количеству завершённых заявок:
 * 0–2 — NEW, 3–9 — REGULAR, 10 и больше — VIP. Правило в {@link ru.mirea.parking.service.LoyaltyCalculator}.
 */
public enum LoyaltyLevel {
    NEW("Новый"),
    REGULAR("Постоянный"),
    VIP("VIP");

    private final String title;

    LoyaltyLevel(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}

package ru.mirea.parking.dto.owner;

/** Общие правила проверки полей владельца для Bean Validation. */
public final class OwnerConstraints {

    /** Тот же формат, что и CHECK у users.phone в V1: +7-XXX-XXX-XX-XX, код 3xx, 4xx, 8xx или 9xx. */
    public static final String PHONE_REGEX = "^\\+7-[3489][0-9]{2}-[0-9]{3}-[0-9]{2}-[0-9]{2}$";
    public static final String PHONE_MESSAGE = "Телефон в формате +7-XXX-XXX-XX-XX";
    public static final String EMAIL_MESSAGE = "Некорректный email";
    public static final String DISCOUNT_MESSAGE = "Скидка от 0 до 50 %";

    private OwnerConstraints() {
    }
}

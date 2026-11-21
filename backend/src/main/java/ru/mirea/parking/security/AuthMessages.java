package ru.mirea.parking.security;

public final class AuthMessages {

    public static final String INVALID_CREDENTIALS = "Неверный логин или пароль";
    public static final String LOGIN_LOCKED = "Слишком много неудачных попыток входа. Попробуйте через 15 минут";
    public static final String ACCOUNT_DISABLED = "Учётная запись отключена";
    public static final String SESSION_EXPIRED = "Сессия истекла, войдите снова";
    public static final String LOGIN_REQUIRED = "Требуется вход в систему";
    public static final String PERMISSION_DENIED = "Недостаточно прав";
    public static final String PASSWORD_CHANGE_REQUIRED = "Смените временный пароль";
    public static final String WRONG_CURRENT_PASSWORD = "Текущий пароль указан неверно";
    public static final String SAME_PASSWORD = "Новый пароль должен отличаться от текущего";

    private AuthMessages() {
    }
}

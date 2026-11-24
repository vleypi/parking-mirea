package ru.mirea.parking.error;

/** Машинные коды ошибок и сообщения по умолчанию. */
public final class ErrorCodes {

    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";
    public static final String STALE_VERSION = "STALE_VERSION";
    public static final String DATA_CONFLICT = "DATA_CONFLICT";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private ErrorCodes() {
    }

    public static String forStatus(int status) {
        return switch (status) {
            case 400 -> BAD_REQUEST;
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 405 -> "METHOD_NOT_ALLOWED";
            case 406 -> "NOT_ACCEPTABLE";
            case 409 -> CONFLICT;
            case 413 -> "PAYLOAD_TOO_LARGE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            case 429 -> "TOO_MANY_REQUESTS";
            default -> status >= 500 ? INTERNAL_ERROR : "CLIENT_ERROR";
        };
    }

    public static String defaultMessage(int status) {
        return switch (status) {
            case 400 -> "Некорректный запрос";
            case 401 -> "Требуется вход в систему";
            case 403 -> "Недостаточно прав";
            case 404 -> "Ресурс не найден";
            case 405 -> "HTTP-метод не поддерживается";
            case 406 -> "Запрошенный формат ответа не поддерживается";
            case 409 -> "Конфликт данных";
            case 413 -> "Запрос или файл слишком большой";
            case 415 -> "Неподдерживаемый формат данных";
            case 429 -> "Слишком много запросов";
            default -> status >= 500 ? "Внутренняя ошибка сервера. Сообщите администратору correlation ID" : "Некорректный запрос";
        };
    }
}

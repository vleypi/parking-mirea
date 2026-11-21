package ru.mirea.parking.logging;

import org.slf4j.MDC;

import java.util.regex.Pattern;

/**
 * Correlation ID связывает HTTP-запрос, строки лога и записи аудита.
 * Значение лежит в MDC под ключом {@link #MDC_KEY}, поэтому его видит и логгер, и AuditService.
 */
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    public static final int MAX_LENGTH = 64;

    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9._-]{1," + MAX_LENGTH + "}");

    private CorrelationId() {
    }

    /** Значение из заголовка принимается, только если оно короткое и без опасных символов. */
    public static boolean isValid(String value) {
        return value != null && ALLOWED.matcher(value).matches();
    }

    public static String current() {
        return MDC.get(MDC_KEY);
    }
}

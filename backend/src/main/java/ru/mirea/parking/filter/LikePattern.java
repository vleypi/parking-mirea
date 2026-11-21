package ru.mirea.parking.filter;

import java.util.Locale;

public final class LikePattern {

    public static final char ESCAPE = '\\';

    private LikePattern() {
    }

    public static String contains(String text) {
        return "%" + escape(text.trim().toLowerCase(Locale.ROOT)) + "%";
    }

    static String escape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}

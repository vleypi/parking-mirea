package ru.mirea.parking.domain;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Формат российского гос. номера (перенесено из InputFormats КР2): латинские двойники заменяются кириллицей. */
public final class LicensePlates {

    public static final String LETTERS = "АВЕКМНОРСТУХ";
    public static final String EXAMPLE = "А123ВС777";
    public static final String FORMAT_MESSAGE = "Гос. номер в формате " + EXAMPLE
            + ": буква, 3 цифры, 2 буквы, регион из 2 или 3 цифр. Буквы: А В Е К М Н О Р С Т У Х";

    private static final String LATIN_LOOKALIKES = "ABEKMHOPCTYX";
    private static final Pattern PLATE = Pattern.compile("[" + LETTERS + "](\\d{3})[" + LETTERS + "]{2}(\\d{2,3})");

    private LicensePlates() {
    }

    /** Верхний регистр, без пробелов и дефисов, латинские буквы-двойники заменены кириллическими. */
    public static String normalizeText(String raw) {
        StringBuilder result = new StringBuilder();
        for (char c : raw.toUpperCase(Locale.ROOT).toCharArray()) {
            if (Character.isWhitespace(c) || c == '-') {
                continue;
            }
            int latin = LATIN_LOOKALIKES.indexOf(c);
            result.append(latin >= 0 ? LETTERS.charAt(latin) : c);
        }
        return result.toString();
    }

    /** Нормализованный номер или null, если формат неверный. */
    public static String normalizeOrNull(String raw) {
        if (raw == null) {
            return null;
        }
        String plate = normalizeText(raw);
        Matcher matcher = PLATE.matcher(plate);
        return matcher.matches() && !matcher.group(1).equals("000") ? plate : null;
    }

    public static boolean isValid(String raw) {
        return normalizeOrNull(raw) != null;
    }

    public static String region(String plate) {
        return plate == null || plate.length() < 7 ? "" : plate.substring(6);
    }
}

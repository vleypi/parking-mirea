package ru.mirea.project.service;

import java.util.regex.Pattern;

import ru.mirea.project.exception.BusinessException;

final class InputFormats {
    static final String PHONE_EXAMPLE = "89991234567";
    static final String PLATE_EXAMPLE = "А123ВС777";

    private static final Pattern PHONE_ALLOWED_CHARS = Pattern.compile("[0-9+()\\s-]+");
    private static final String RUSSIAN_MOBILE_AND_CITY_CODES = "3489";

    private static final String PLATE_LETTERS = "АВЕКМНОРСТУХ";
    private static final String LATIN_LOOKALIKES = "ABEKMHOPCTYX";
    private static final Pattern PLATE_PATTERN =
        Pattern.compile("[" + PLATE_LETTERS + "](\\d{3})[" + PLATE_LETTERS + "]{2}\\d{2,3}");

    private static final Pattern NAME_PATTERN = Pattern.compile("\\p{L}+([ .'-]\\p{L}*)*");

    private InputFormats() {
    }

    static String normalizePhone(String raw) {
        String input = raw.trim();
        if (!PHONE_ALLOWED_CHARS.matcher(input).matches() || input.lastIndexOf('+') > 0) {
            throw invalidPhone(input);
        }

        String digits = input.replaceAll("\\D", "");
        if (digits.length() == 11 && (digits.charAt(0) == '8' || digits.charAt(0) == '7')) {
            if (input.startsWith("+") && digits.charAt(0) != '7') {
                throw invalidPhone(input);
            }
            digits = digits.substring(1);
        } else if (digits.length() != 10 || input.startsWith("+")) {
            throw invalidPhone(input);
        }

        if (RUSSIAN_MOBILE_AND_CITY_CODES.indexOf(digits.charAt(0)) < 0) {
            throw invalidPhone(input);
        }

        return "+7-%s-%s-%s-%s".formatted(
            digits.substring(0, 3), digits.substring(3, 6), digits.substring(6, 8), digits.substring(8));
    }

    static String normalizePlateText(String raw) {
        StringBuilder result = new StringBuilder();
        for (char c : raw.toUpperCase().toCharArray()) {
            if (Character.isWhitespace(c) || c == '-') {
                continue;
            }
            int latinIndex = LATIN_LOOKALIKES.indexOf(c);
            result.append(latinIndex >= 0 ? PLATE_LETTERS.charAt(latinIndex) : c);
        }
        return result.toString();
    }

    static String normalizePlate(String raw) {
        String plate = normalizePlateText(raw);
        var matcher = PLATE_PATTERN.matcher(plate);
        if (!matcher.matches() || matcher.group(1).equals("000")) {
            throw new BusinessException("Некорректный гос. номер «" + raw.trim() + "». Принимаются только российские номера"
                + " формата " + PLATE_EXAMPLE + ": буква, 3 цифры, 2 буквы, регион из 2 или 3 цифр."
                + " Допустимые буквы: " + String.join(" ", PLATE_LETTERS.split("")));
        }
        return plate;
    }

    static String normalizeName(String raw) {
        String name = raw.trim().replaceAll("\\s+", " ");
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new BusinessException("Имя может содержать только буквы, пробелы, дефис, точку и апостроф");
        }
        return name;
    }

    private static BusinessException invalidPhone(String input) {
        return new BusinessException("Некорректный номер телефона «" + input + "». Принимаются только российские номера:"
            + " 11 цифр с 8 или +7 в начале, либо 10 цифр без кода страны."
            + " Например: " + PHONE_EXAMPLE + ", +79991234567, 9991234567");
    }
}

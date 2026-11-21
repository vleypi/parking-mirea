package ru.mirea.parking.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class TemporaryPasswordGenerator {

    private static final String LETTERS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITS = "23456789";
    private static final String ALPHABET = LETTERS + DIGITS;
    private static final int LENGTH = 12;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder password = new StringBuilder(LENGTH);
        password.append(pick(LETTERS)).append(pick(DIGITS));
        while (password.length() < LENGTH) {
            password.append(pick(ALPHABET));
        }
        return shuffle(password);
    }

    private char pick(String source) {
        return source.charAt(random.nextInt(source.length()));
    }

    private String shuffle(StringBuilder value) {
        char[] chars = value.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }
}

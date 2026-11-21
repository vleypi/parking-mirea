package ru.mirea.parking.security;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 10;
    public static final int MAX_LENGTH = 72;

    public List<String> violations(String password, String login) {
        List<String> errors = new ArrayList<>();
        if (password == null || password.length() < MIN_LENGTH) {
            errors.add("Пароль должен быть не короче " + MIN_LENGTH + " символов");
        }
        if (password != null && password.length() > MAX_LENGTH) {
            errors.add("Пароль должен быть не длиннее " + MAX_LENGTH + " символов");
        }
        if (password == null || password.chars().noneMatch(Character::isLetter)) {
            errors.add("Пароль должен содержать хотя бы одну букву");
        }
        if (password == null || password.chars().noneMatch(Character::isDigit)) {
            errors.add("Пароль должен содержать хотя бы одну цифру");
        }
        if (password != null && login != null && password.equalsIgnoreCase(login)) {
            errors.add("Пароль не должен совпадать с логином");
        }
        return errors;
    }
}

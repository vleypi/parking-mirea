package ru.mirea.parking.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @ParameterizedTest
    @ValueSource(strings = {"parking2026", "Парковка2026", "a1b2c3d4e5", "0123456789x"})
    void acceptsValidPasswords(String password) {
        assertThat(policy.violations(password, "operator")).isEmpty();
    }

    @Test
    void rejectsShortPassword() {
        assertThat(policy.violations("abc12345", "operator"))
                .containsExactly("Пароль должен быть не короче 10 символов");
    }

    @Test
    void acceptsExactlyMinLength() {
        assertThat(policy.violations("abcdefghi1", "operator")).isEmpty();
    }

    @Test
    void rejectsPasswordWithoutDigits() {
        assertThat(policy.violations("onlylettershere", "operator"))
                .containsExactly("Пароль должен содержать хотя бы одну цифру");
    }

    @Test
    void rejectsPasswordWithoutLetters() {
        assertThat(policy.violations("1234567890", "operator"))
                .containsExactly("Пароль должен содержать хотя бы одну букву");
    }

    @Test
    void rejectsPasswordEqualToLogin() {
        assertThat(policy.violations("Operator2026", "operator2026"))
                .containsExactly("Пароль не должен совпадать с логином");
    }

    @Test
    void rejectsTooLongPassword() {
        assertThat(policy.violations("a1".repeat(37), "operator"))
                .containsExactly("Пароль должен быть не длиннее 72 символов");
    }

    @Test
    void reportsAllViolationsAtOnce() {
        assertThat(policy.violations("", "operator")).hasSize(3);
        assertThat(policy.violations(null, "operator")).hasSize(3);
    }
}

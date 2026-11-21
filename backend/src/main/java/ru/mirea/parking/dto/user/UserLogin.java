package ru.mirea.parking.dto.user;

final class UserLogin {

    static final String PATTERN = "^[A-Za-z0-9._-]+$";
    static final String MESSAGE = "Только латинские буквы, цифры, точка, дефис и подчеркивание";

    private UserLogin() {
    }
}

package ru.mirea.parking.domain;

public enum Role {
    VIEWER,
    OPERATOR,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}

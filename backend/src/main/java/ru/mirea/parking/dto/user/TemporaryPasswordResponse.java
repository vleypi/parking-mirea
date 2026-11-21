package ru.mirea.parking.dto.user;

public record TemporaryPasswordResponse(UserAccountResponse user, String temporaryPassword) {
}

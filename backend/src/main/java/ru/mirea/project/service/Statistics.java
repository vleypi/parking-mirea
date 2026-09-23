package ru.mirea.project.service;

public record Statistics(
    long totalUsers,
    long totalRequests,
    long active,
    long completed,
    long cancelled,
    long occupiedNow
) {
}

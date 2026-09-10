package ru.mirea.project.model;

import java.time.LocalDateTime;

public class User {
    private long id;
    private String name;
    private String phone;
    private LocalDateTime createdAt;

    public User(long id, String name, String phone, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "[%d] %s (%s)".formatted(id, name, phone);
    }
}

package ru.mirea.project.model;

public class Vehicle {
    private long id;
    private long userId;
    private String licensePlate;
    private String brand;
    private String model;

    public Vehicle(long id, long userId, String licensePlate, String brand, String model) {
        this.id = id;
        this.userId = userId;
        this.licensePlate = licensePlate;
        this.brand = brand;
        this.model = model;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public String toString() {
        return "[%d] %s | %s %s | владелец: %d".formatted(id, licensePlate, brand, model, userId);
    }
}

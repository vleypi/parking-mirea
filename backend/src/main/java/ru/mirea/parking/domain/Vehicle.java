package ru.mirea.parking.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Автомобиль владельца (таблица vehicles). Внешний идентификатор в API — uuid, для импорта — гос. номер. */
@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Owner owner;

    @Column(name = "license_plate", nullable = false, unique = true, length = 12)
    private String licensePlate;

    /** Цифровая часть номера (А123ВС777 → 123): по ней сортировка, как в КР2. */
    @Formula("substring(license_plate, 2, 3)")
    private String plateDigits;

    /** Регион из номера (А123ВС777 → 777). */
    @Formula("substring(license_plate, 7)")
    private String region;

    @Column(nullable = false, length = 50)
    private String brand;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(length = 30)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @ColumnDefault("'ACTIVE'")
    private VehicleStatus status = VehicleStatus.ACTIVE;

    @Column(name = "release_year")
    private Short releaseYear;

    @Column(name = "is_electric", nullable = false)
    @ColumnDefault("false")
    private boolean electric;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean archived;

    @Column(name = "created_at", nullable = false, updatable = false)
    @ColumnDefault("now()")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @ColumnDefault("now()")
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    @ColumnDefault("0")
    private long version;

    @BatchSize(size = 100)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "vehicle_tag", joinColumns = @JoinColumn(name = "vehicle_id"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new LinkedHashSet<>();

    protected Vehicle() {
    }

    public Vehicle(Owner owner, String licensePlate, String brand, String model) {
        this.owner = owner;
        this.licensePlate = licensePlate;
        this.brand = brand;
        this.model = model;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public UUID getUuid() { return uuid; }

    public Owner getOwner() { return owner; }
    public void setOwner(Owner owner) { this.owner = owner; }
    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public String getRegion() { return LicensePlates.region(licensePlate); }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public VehicleStatus getStatus() { return status; }
    public void setStatus(VehicleStatus status) { this.status = status; }
    public Short getReleaseYear() { return releaseYear; }
    public void setReleaseYear(Short releaseYear) { this.releaseYear = releaseYear; }
    public boolean isElectric() { return electric; }
    public void setElectric(boolean electric) { this.electric = electric; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = new LinkedHashSet<>(tags); }
}

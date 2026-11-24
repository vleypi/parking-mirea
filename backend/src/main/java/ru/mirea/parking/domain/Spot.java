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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Парковочное место (таблица spots). Внешний идентификатор для импорта — номер места. */
@Entity
@Table(name = "spots")
public class Spot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(name = "spot_number", nullable = false, unique = true)
    private Integer spotNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_type_id", nullable = false)
    private SpotType spotType;

    @Column(name = "hourly_rate", nullable = false, precision = 8, scale = 2)
    private BigDecimal hourlyRate;

    @Column(length = 20)
    private String zone;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @ColumnDefault("'AVAILABLE'")
    private SpotStatus status = SpotStatus.AVAILABLE;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean covered;

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
    @CollectionTable(name = "spot_tag", joinColumns = @JoinColumn(name = "spot_id"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new LinkedHashSet<>();

    protected Spot() {
    }

    public Spot(Integer spotNumber, SpotType spotType, BigDecimal hourlyRate) {
        this.spotNumber = spotNumber;
        this.spotType = spotType;
        this.hourlyRate = hourlyRate;
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

    public Integer getSpotNumber() { return spotNumber; }
    public void setSpotNumber(Integer spotNumber) { this.spotNumber = spotNumber; }
    public SpotType getSpotType() { return spotType; }
    public void setSpotType(SpotType spotType) { this.spotType = spotType; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public SpotStatus getStatus() { return status; }
    public void setStatus(SpotStatus status) { this.status = status; }
    public boolean isCovered() { return covered; }
    public void setCovered(boolean covered) { this.covered = covered; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) {
        this.tags.clear();
        this.tags.addAll(tags);
    }
}

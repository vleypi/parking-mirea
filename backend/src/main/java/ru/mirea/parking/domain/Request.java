package ru.mirea.parking.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.Generated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "requests")
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    @Generated
    @Column(insertable = false, updatable = false, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private Owner owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id", nullable = false)
    private Spot spot;

    @ManyToOne(optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private RequestStatus status;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean paid;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "request_tag", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new LinkedHashSet<>();

    protected Request() {
    }

    public Request(Owner owner, Vehicle vehicle, Spot spot, RequestStatus status,
                   LocalDateTime startTime, LocalDateTime endTime) {
        this.owner = owner;
        this.vehicle = vehicle;
        this.spot = spot;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
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
    public String getCode() { return code; }

    public Owner getOwner() { return owner; }
    public void setOwner(Owner owner) { this.owner = owner; }
    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
    public Spot getSpot() { return spot; }
    public void setSpot(Spot spot) { this.spot = spot; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = new LinkedHashSet<>(tags); }
}

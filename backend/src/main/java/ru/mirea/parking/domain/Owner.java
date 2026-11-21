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

/**
 * Владелец автомобилей (клиент парковки), таблица users из КР1.
 * Называется Owner, чтобы не путать с учётной записью сотрудника {@link UserAccount}.
 * <p>
 * Внешний идентификатор в REST API — uuid, для импорта CSV — телефон (он уникален).
 * {@code @ColumnDefault} повторяет DEFAULT из миграции V11, чтобы тестовая схема H2 вела себя так же.
 */
@Entity
@Table(name = "users")
public class Owner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @ColumnDefault("1")
    private ClientCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @ColumnDefault("'ACTIVE'")
    private OwnerStatus status = OwnerStatus.ACTIVE;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    @ColumnDefault("0")
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "notifications_enabled", nullable = false)
    @ColumnDefault("true")
    private boolean notificationsEnabled = true;

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
    @CollectionTable(name = "owner_tag", joinColumns = @JoinColumn(name = "owner_id"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new LinkedHashSet<>();

    protected Owner() {
    }

    public Owner(String name, String phone, ClientCategory category) {
        this.name = name;
        this.phone = phone;
        this.category = category;
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

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public ClientCategory getCategory() { return category; }
    public void setCategory(ClientCategory category) { this.category = category; }
    public OwnerStatus getStatus() { return status; }
    public void setStatus(OwnerStatus status) { this.status = status; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(BigDecimal discountPercent) { this.discountPercent = discountPercent; }
    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }

    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = new LinkedHashSet<>(tags); }
}

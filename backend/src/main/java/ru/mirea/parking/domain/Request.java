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

/**
 * Заявка на парковку: основная сущность системы (таблица requests, миграции V1 и V10).
 *
 * <p>Наружу через REST эта сущность не отдаётся: контроллер возвращает RequestResponse (DTO).
 * Удаление заменено архивированием: флаг {@link #archived}.</p>
 */
@Entity
@Table(name = "requests")
public class Request {

    /** Внутренний ключ БД. Через него связаны другие таблицы, наружу не показывается. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Внешний идентификатор для API: /api/requests/{uuid}.
     * Генерируем в Java сразу при создании объекта, чтобы он был известен ещё до сохранения.
     */
    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid = UUID.randomUUID();

    /**
     * Номер заявки REQ-000001. Его выдаёт база: DEFAULT из последовательности request_code_seq.
     * insertable = false: Hibernate не пишет столбец в INSERT, чтобы сработал DEFAULT.
     * {@code @Generated}: после INSERT Hibernate перечитывает значение из БД в это поле.
     */
    @Generated
    @Column(insertable = false, updatable = false, length = 20)
    private String code;

    /**
     * Владелец. LAZY: сам объект User загрузится из БД, только когда к нему обратятся,
     * а не при каждом чтении заявки.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spot_id", nullable = false)
    private Spot spot;

    /** Статус из справочника request_statuses. EAGER (по умолчанию у ManyToOne): он почти всегда нужен. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "status_id", nullable = false)
    private RequestStatus status;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(length = 500)
    private String description;

    /** Деньги храним в BigDecimal, а не double: double даёт ошибки округления (0.1 + 0.2 != 0.3). */
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

    /**
     * Optimistic locking. Hibernate добавляет к UPDATE условие "WHERE version = ?" и увеличивает version.
     * Если запись уже изменил кто-то другой, строка не найдётся и будет
     * OptimisticLockException, которую мы превратим в ответ 409.
     */
    @Version
    @Column(nullable = false)
    private long version;

    /**
     * Теги: простые строки без отдельной сущности. Хранятся в таблице request_tag
     * (request_id, tag). Set, а не List: один тег у заявки может быть только один раз.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "request_tag", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new LinkedHashSet<>();

    /** Нужен Hibernate для создания объекта при чтении из БД. protected: снаружи им не пользуются. */
    protected Request() {
    }

    public Request(User owner, Vehicle vehicle, Spot spot, RequestStatus status,
                   LocalDateTime startTime, LocalDateTime endTime) {
        this.owner = owner;
        this.vehicle = vehicle;
        this.spot = spot;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /** Вызывается Hibernate перед первым INSERT. */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    /** Вызывается Hibernate перед каждым UPDATE. */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public UUID getUuid() { return uuid; }
    public String getCode() { return code; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
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

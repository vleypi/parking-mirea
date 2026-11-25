package ru.mirea.parking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;

/** Тип парковочного места (справочник spot_types): Стандартное, Для людей с инвалидностью, Для электромобилей. */
@Entity
@Table(name = "spot_types")
public class SpotType {

    @Id
    private Short id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, unique = true, length = 50)
    private String title;

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @ColumnDefault("0")
    private short sortOrder;

    protected SpotType() {
    }

    public SpotType(Short id, String code, String title, short sortOrder) {
        this.id = id;
        this.code = code;
        this.title = title;
        this.sortOrder = sortOrder;
    }

    public Short getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public short getSortOrder() { return sortOrder; }
    public void setSortOrder(short sortOrder) { this.sortOrder = sortOrder; }
}

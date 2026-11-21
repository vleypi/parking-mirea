package ru.mirea.parking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "spot_types")
public class SpotType {

    @Id
    private Short id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, unique = true, length = 50)
    private String title;

    protected SpotType() {
    }

    public Short getId() { return id; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
}

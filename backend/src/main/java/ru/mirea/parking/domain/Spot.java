package ru.mirea.parking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Парковочное место (таблица spots из КР1).
 *
 * <p>МИНИМАЛЬНАЯ ЗАГОТОВКА: создана в задаче Request для связи {@code @ManyToOne}.
 * Сущность принадлежит задаче «Миграция и JPA-сущность (Spot)»: Руслан дописывает сюда свои поля
 * (тип места через справочник и т.д.).</p>
 */
@Entity
@Table(name = "spots")
public class Spot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spot_number", nullable = false, unique = true)
    private Integer spotNumber;

    /** Тариф за час. Из него считается стоимость заявки. */
    @Column(name = "hourly_rate", nullable = false, precision = 8, scale = 2)
    private BigDecimal hourlyRate;

    protected Spot() {
    }

    public Long getId() { return id; }
    public Integer getSpotNumber() { return spotNumber; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
}

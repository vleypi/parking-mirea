package ru.mirea.parking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Статус заявки: строка справочника request_statuses (NEW, CONFIRMED, COMPLETED, CANCELLED).
 *
 * <p>Справочник только читается, строки заданы в миграции V1. Поэтому у класса нет сеттеров,
 * а id не генерируется: он совпадает с id из таблицы (1..4).</p>
 */
@Entity
@Table(name = "request_statuses")
public class RequestStatus {

    public static final String NEW = "NEW";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";

    /** В таблице SMALLINT, в Java ему соответствует Short. */
    @Id
    private Short id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, unique = true, length = 50)
    private String title;

    protected RequestStatus() {
    }

    public Short getId() { return id; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
}

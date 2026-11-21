package ru.mirea.parking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Категория клиента (справочник client_categories из V11): Обычный, Резидент, Сотрудник.
 * Отключённую категорию нельзя выбрать для владельца, но существующие ссылки на неё остаются.
 */
@Entity
@Table(name = "client_categories")
public class ClientCategory {

    /** id «Обычный»: он же DEFAULT для users.category_id, чтобы JavaFX-версия могла вставлять владельцев. */
    public static final short DEFAULT_ID = 1;

    @Id
    private Short id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, unique = true, length = 50)
    private String title;

    @Column(nullable = false)
    private boolean active = true;

    protected ClientCategory() {
    }

    public ClientCategory(Short id, String code, String title) {
        this.id = id;
        this.code = code;
        this.title = title;
    }

    public Short getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

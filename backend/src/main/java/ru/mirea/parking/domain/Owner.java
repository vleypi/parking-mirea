package ru.mirea.parking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Владелец автомобиля (таблица users из КР1).
 *
 * <p>Класс называется Owner, а не User, чтобы не путать владельца машины с учётной записью
 * для входа в систему (UserAccount, задача «Вход, JWT и refresh-сессии»).</p>
 *
 * <p>МИНИМАЛЬНАЯ ЗАГОТОВКА: создана в задаче Request, чтобы на неё можно было сослаться
 * через {@code @ManyToOne}. Сущность целиком принадлежит задаче «Миграция и JPA-сущность (User)»:
 * Иракли дописывает сюда свои поля (uuid, archived, version...), а не создаёт класс заново.</p>
 */
@Entity
@Table(name = "users")
public class Owner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Конструктор без параметров нужен Hibernate, чтобы создавать объект при чтении из БД. */
    protected Owner() {
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

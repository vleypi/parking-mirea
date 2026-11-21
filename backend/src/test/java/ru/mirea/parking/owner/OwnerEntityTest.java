package ru.mirea.parking.owner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.parking.domain.ClientCategory;
import ru.mirea.parking.domain.Owner;
import ru.mirea.parking.domain.OwnerStatus;
import ru.mirea.parking.repository.ClientCategoryRepository;
import ru.mirea.parking.repository.OwnerRepository;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OwnerEntityTest {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private OwnerRepository owners;
    @Autowired
    private ClientCategoryRepository categories;

    @BeforeEach
    void seed() {
        OwnerTestData.clean(jdbc);
    }

    @Test
    void legacyInsertWithoutNewColumnsGetsDefaults() {
        long id = OwnerTestData.insertLegacy(jdbc, "Иван Иванов", "+7-900-100-00-01");

        Owner owner = owners.findById(id).orElseThrow();
        assertThat(owner.getUuid()).isNotNull();
        assertThat(owner.getCategory().getTitle()).isEqualTo("Обычный");
        assertThat(owner.getStatus()).isEqualTo(OwnerStatus.ACTIVE);
        assertThat(owner.getDiscountPercent()).isEqualByComparingTo("0");
        assertThat(owner.isNotificationsEnabled()).isTrue();
        assertThat(owner.isArchived()).isFalse();
        assertThat(owner.getVersion()).isZero();
        assertThat(owner.getTags()).isEmpty();
    }

    @Test
    void savedOwnerKeepsAllFieldsAndIncrementsVersion() {
        ClientCategory resident = categories.findById((short) 2).orElseThrow();
        Owner owner = new Owner("Мария Петрова", "+7-900-100-00-02", resident);
        owner.setEmail("maria@example.com");
        owner.setDiscountPercent(new BigDecimal("15.50"));
        owner.setNotificationsEnabled(false);
        owner.setTags(Set.of("vip"));
        owner = owners.saveAndFlush(owner);
        assertThat(owner.getCreatedAt()).isNotNull();
        assertThat(owner.getVersion()).isZero();

        owner.setStatus(OwnerStatus.BLOCKED);
        owner = owners.saveAndFlush(owner);

        Owner loaded = owners.findByUuid(owner.getUuid()).orElseThrow();
        assertThat(loaded.getCategory().getCode()).isEqualTo("RESIDENT");
        assertThat(loaded.getStatus()).isEqualTo(OwnerStatus.BLOCKED);
        assertThat(loaded.getDiscountPercent()).isEqualByComparingTo("15.50");
        assertThat(loaded.getTags()).containsExactly("vip");
        assertThat(loaded.getVersion()).isEqualTo(1);
    }
}

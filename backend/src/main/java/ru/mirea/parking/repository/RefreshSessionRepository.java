package ru.mirea.parking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.mirea.parking.domain.RefreshSession;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

    Optional<RefreshSession> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshSession s set s.revokedAt = :now where s.user.id = :userId and s.revokedAt is null")
    int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}

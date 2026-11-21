package ru.mirea.parking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.parking.domain.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByLogin(String login);
}

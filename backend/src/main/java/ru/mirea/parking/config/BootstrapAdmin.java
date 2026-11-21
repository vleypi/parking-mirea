package ru.mirea.parking.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.mirea.parking.domain.Role;
import ru.mirea.parking.domain.UserAccount;
import ru.mirea.parking.repository.UserAccountRepository;
import ru.mirea.parking.security.PasswordPolicy;
import ru.mirea.parking.service.AuthService;

import java.util.List;
import java.util.Set;

@Component
public class BootstrapAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdmin.class);

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final AppProperties.Bootstrap bootstrap;

    public BootstrapAdmin(UserAccountRepository users, PasswordEncoder passwordEncoder,
                          PasswordPolicy passwordPolicy, AppProperties properties) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.bootstrap = properties.bootstrap();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (isBlank(bootstrap.login()) || isBlank(bootstrap.password()) || users.count() > 0) {
            return;
        }
        String login = AuthService.normalizeLogin(bootstrap.login());
        List<String> violations = passwordPolicy.violations(bootstrap.password(), login);
        if (!violations.isEmpty()) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD не проходит политику паролей: " + violations);
        }
        UserAccount admin = new UserAccount(login, bootstrap.name(), passwordEncoder.encode(bootstrap.password()),
                Set.of(Role.ADMIN));
        admin.setMustChangePassword(false);
        users.save(admin);
        log.info("Создан первый администратор: {}", login);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

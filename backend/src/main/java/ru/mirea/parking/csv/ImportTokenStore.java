package ru.mirea.parking.csv;

import org.springframework.stereotype.Component;
import ru.mirea.parking.error.BadRequestException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Хранит проверенный файл импорта по importToken до apply (30 минут), токен привязан к сущности и пользователю. */
@Component
public class ImportTokenStore {

    public static final Duration TTL = Duration.ofMinutes(30);

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Clock clock;

    public ImportTokenStore(Clock clock) {
        this.clock = clock;
    }

    private record Entry(String entity, String user, byte[] content, Instant expiresAt) {
    }

    public record Stored(String token, Instant expiresAt) {
    }

    public Stored put(String entity, String user, byte[] content) {
        removeExpired();
        Entry entry = new Entry(entity, user, content, clock.instant().plus(TTL));
        String token = UUID.randomUUID().toString();
        entries.put(token, entry);
        return new Stored(token, entry.expiresAt());
    }

    /** Забирает файл: второй apply с тем же токеном невозможен. */
    public byte[] take(String token, String entity, String user) {
        removeExpired();
        Entry entry = entries.get(token);
        if (entry == null || !entry.entity().equals(entity) || !entry.user().equals(user)) {
            throw new BadRequestException("importToken",
                    "Токен импорта не найден или истёк. Загрузите файл и выполните проверку заново");
        }
        entries.remove(token);
        return entry.content();
    }

    private void removeExpired() {
        Instant now = clock.instant();
        entries.values().removeIf(entry -> entry.expiresAt().isBefore(now));
    }
}

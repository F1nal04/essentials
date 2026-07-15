package f1nal.essentials.moderation;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ModerationService implements AutoCloseable {

    private final ModerationDatabase database;
    private final Clock clock;
    private final ConcurrentMap<UUID, BanRecord> activeBans = new ConcurrentHashMap<>();

    private ModerationService(ModerationDatabase database, Clock clock) throws SQLException {
        this.database = database;
        this.clock = clock;
        for (BanRecord ban : database.loadActiveBans(clock.millis())) {
            activeBans.put(ban.targetUuid(), ban);
        }
    }

    public static ModerationService open(Path databasePath, Clock clock) throws IOException, SQLException {
        ModerationDatabase database = ModerationDatabase.open(databasePath, clock);
        try {
            return new ModerationService(database, clock);
        } catch (SQLException | RuntimeException e) {
            try {
                database.close();
            } catch (SQLException closeError) {
                e.addSuppressed(closeError);
            }
            throw e;
        }
    }

    public Optional<BanRecord> ban(
            UUID targetUuid,
            String targetName,
            String reason,
            long durationMs,
            Moderator moderator) throws SQLException {
        long issuedAtMs = clock.millis();
        long expiresAtMs;
        try {
            expiresAtMs = Math.addExact(issuedAtMs, durationMs);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Ban expiration is too far in the future", e);
        }
        Optional<BanRecord> inserted = database.insertBan(
                targetUuid, targetName, reason, issuedAtMs, expiresAtMs, moderator);
        inserted.ifPresent(ban -> activeBans.put(targetUuid, ban));
        return inserted;
    }

    public Optional<BanRecord> activeBan(UUID targetUuid) {
        BanRecord ban = activeBans.get(targetUuid);
        if (ban == null) {
            return Optional.empty();
        }
        if (ban.expiresAtMs() <= clock.millis()) {
            activeBans.remove(targetUuid, ban);
            return Optional.empty();
        }
        return Optional.of(ban);
    }

    public void logKick(UUID targetUuid, String targetName, String reason, Moderator moderator)
            throws SQLException {
        database.insertKick(targetUuid, targetName, reason, clock.millis(), moderator);
    }

    public long nowMs() {
        return clock.millis();
    }

    @Override
    public void close() throws SQLException {
        activeBans.clear();
        database.close();
    }
}

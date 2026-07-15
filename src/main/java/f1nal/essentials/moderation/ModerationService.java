package f1nal.essentials.moderation;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ModerationService implements AutoCloseable {

    private final ModerationDatabase database;
    private final Clock clock;
    private final ConcurrentMap<UUID, BanRecord> activeBans = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, IpBanRecord> activeIpBans = new ConcurrentHashMap<>();

    private ModerationService(ModerationDatabase database, Clock clock) throws SQLException {
        this.database = database;
        this.clock = clock;
        for (BanRecord ban : database.loadActiveBans(clock.millis())) {
            activeBans.put(ban.targetUuid(), ban);
        }
        for (IpBanRecord ban : database.loadActiveIpBans(clock.millis())) {
            activeIpBans.put(ban.address(), ban);
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

    public Optional<IpBanRecord> banIp(
            String address,
            String reason,
            long durationMs,
            Moderator moderator) throws SQLException {
        String normalizedAddress = IpAddressUtil.normalizeLiteral(address);
        long issuedAtMs = clock.millis();
        long expiresAtMs = expiration(issuedAtMs, durationMs, "IP-ban");
        Optional<IpBanRecord> inserted = database.insertIpBan(
                normalizedAddress, null, null, reason,
                issuedAtMs, expiresAtMs, moderator);
        inserted.ifPresent(ban -> activeIpBans.put(normalizedAddress, ban));
        return inserted;
    }

    public Optional<PlayerIpBanResult> banPlayerIp(
            String address,
            UUID targetUuid,
            String targetName,
            String reason,
            long durationMs,
            Moderator moderator) throws SQLException {
        String normalizedAddress = IpAddressUtil.normalizeLiteral(address);
        long issuedAtMs = clock.millis();
        long expiresAtMs = expiration(issuedAtMs, durationMs, "Player/IP ban");
        Optional<PlayerIpBanResult> inserted = database.insertPlayerIpBan(
                normalizedAddress, targetUuid, targetName, reason,
                issuedAtMs, expiresAtMs, moderator);
        inserted.ifPresent(result -> {
            activeBans.put(targetUuid, result.playerBan());
            activeIpBans.put(normalizedAddress, result.ipBan());
        });
        return inserted;
    }

    public Optional<IpBanRecord> activeIpBan(String address) {
        String normalizedAddress;
        try {
            normalizedAddress = IpAddressUtil.normalizeLiteral(address);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        IpBanRecord ban = activeIpBans.get(normalizedAddress);
        if (ban == null) {
            return Optional.empty();
        }
        if (ban.expiresAtMs() <= clock.millis()) {
            activeIpBans.remove(normalizedAddress, ban);
            return Optional.empty();
        }
        return Optional.of(ban);
    }

    public List<IpBanRecord> activeIpBans(UUID targetUuid) {
        return activeIpBans.values().stream()
                .filter(ban -> targetUuid.equals(ban.targetUuid()))
                .filter(ban -> activeIpBan(ban.address()).isPresent())
                .sorted(Comparator.comparingLong(IpBanRecord::expiresAtMs))
                .toList();
    }

    public void logKick(UUID targetUuid, String targetName, String reason, Moderator moderator)
            throws SQLException {
        database.insertKick(targetUuid, targetName, reason, clock.millis(), moderator);
    }

    public AuditPage history(UUID targetUuid, AuditFilter filter, int limit, int offset)
            throws SQLException {
        return database.loadHistory(targetUuid, filter, limit, offset);
    }

    public long nowMs() {
        return clock.millis();
    }

    private static long expiration(long issuedAtMs, long durationMs, String action) {
        try {
            return Math.addExact(issuedAtMs, durationMs);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(action + " expiration is too far in the future", e);
        }
    }

    @Override
    public void close() throws SQLException {
        activeBans.clear();
        activeIpBans.clear();
        database.close();
    }
}

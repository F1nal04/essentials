package f1nal.essentials.moderation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Owns the single SQLite connection and the current moderation schema. */
public final class ModerationDatabase implements AutoCloseable {

    private final Connection connection;
    private final Clock clock;

    private ModerationDatabase(Connection connection, Clock clock) {
        this.connection = connection;
        this.clock = clock;
    }

    public static ModerationDatabase open(Path path, Clock clock) throws IOException, SQLException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver is not available", e);
        }

        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
        ModerationDatabase database = new ModerationDatabase(connection, clock);
        try {
            database.configure();
            database.initializeSchema();
            return database;
        } catch (SQLException | RuntimeException e) {
            try {
                connection.close();
            } catch (SQLException closeError) {
                e.addSuppressed(closeError);
            }
            throw e;
        }
    }

    private void configure() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("PRAGMA journal_mode = DELETE");
            statement.execute("PRAGMA synchronous = FULL");
        }
    }

    private void initializeSchema() throws SQLException {
        inTransaction(() -> {
            if (expirationIsRequired("bans")) {
                migrateBansForPermanentExpirations();
            }
            if (expirationIsRequired("ip_bans")) {
                migrateIpBansForPermanentExpirations();
            }
            createCurrentSchema();
            return null;
        });
    }

    private boolean expirationIsRequired(String table) throws SQLException {
        String safeTable = switch (table) {
            case "bans" -> "bans";
            case "ip_bans" -> "ip_bans";
            default -> throw new IllegalArgumentException("Unexpected moderation table: " + table);
        };
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("PRAGMA table_info(" + safeTable + ")")) {
            while (result.next()) {
                if ("expires_at_ms".equals(result.getString("name"))) {
                    return result.getInt("notnull") == 1;
                }
            }
            return false;
        }
    }

    private void migrateBansForPermanentExpirations() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP INDEX IF EXISTS bans_one_active_per_player");
            statement.execute("DROP INDEX IF EXISTS bans_active_expiration");
            statement.execute("DROP INDEX IF EXISTS bans_target_time");
            statement.execute("ALTER TABLE bans RENAME TO bans_before_permanent");
            createBansTable(statement);
            statement.execute("""
                    INSERT INTO bans(
                        id, target_uuid, target_name, reason, issued_at_ms, expires_at_ms,
                        moderator_uuid, moderator_name, state, revoked_at_ms,
                        revoked_by_uuid, revoked_by_name
                    )
                    SELECT id, target_uuid, target_name, reason, issued_at_ms, expires_at_ms,
                           moderator_uuid, moderator_name, state, revoked_at_ms,
                           revoked_by_uuid, revoked_by_name
                    FROM bans_before_permanent
                    """);
            statement.execute("DROP TABLE bans_before_permanent");
        }
    }

    private void migrateIpBansForPermanentExpirations() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP INDEX IF EXISTS ip_bans_one_active_per_address");
            statement.execute("DROP INDEX IF EXISTS ip_bans_active_expiration");
            statement.execute("DROP INDEX IF EXISTS ip_bans_target_time");
            statement.execute("ALTER TABLE ip_bans RENAME TO ip_bans_before_permanent");
            createIpBansTable(statement);
            statement.execute("""
                    INSERT INTO ip_bans(
                        id, address, target_uuid, target_name, reason, issued_at_ms,
                        expires_at_ms, moderator_uuid, moderator_name, state,
                        revoked_at_ms, revoked_by_uuid, revoked_by_name
                    )
                    SELECT id, address, target_uuid, target_name, reason, issued_at_ms,
                           expires_at_ms, moderator_uuid, moderator_name, state,
                           revoked_at_ms, revoked_by_uuid, revoked_by_name
                    FROM ip_bans_before_permanent
                    """);
            statement.execute("DROP TABLE ip_bans_before_permanent");
        }
    }

    private void createCurrentSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            createBansTable(statement);
            statement.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS bans_one_active_per_player
                    ON bans(target_uuid) WHERE state = 'ACTIVE'
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS bans_active_expiration
                    ON bans(state, expires_at_ms)
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS bans_target_time
                    ON bans(target_uuid, issued_at_ms DESC)
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS kicks (
                        id INTEGER PRIMARY KEY,
                        target_uuid TEXT NOT NULL,
                        target_name TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        kicked_at_ms INTEGER NOT NULL,
                        moderator_uuid TEXT,
                        moderator_name TEXT NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS kicks_target_time
                    ON kicks(target_uuid, kicked_at_ms DESC)
                    """);
            createIpBansTable(statement);
            statement.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS ip_bans_one_active_per_address
                    ON ip_bans(address) WHERE state = 'ACTIVE'
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS ip_bans_active_expiration
                    ON ip_bans(state, expires_at_ms)
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS ip_bans_target_time
                    ON ip_bans(target_uuid, issued_at_ms DESC)
                    """);
        }
    }

    private static void createBansTable(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS bans (
                    id INTEGER PRIMARY KEY,
                    target_uuid TEXT NOT NULL,
                    target_name TEXT NOT NULL,
                    reason TEXT NOT NULL,
                    issued_at_ms INTEGER NOT NULL,
                    expires_at_ms INTEGER,
                    moderator_uuid TEXT,
                    moderator_name TEXT NOT NULL,
                    state TEXT NOT NULL DEFAULT 'ACTIVE'
                        CHECK (state IN ('ACTIVE', 'EXPIRED', 'REVOKED')),
                    revoked_at_ms INTEGER,
                    revoked_by_uuid TEXT,
                    revoked_by_name TEXT,
                    CHECK (expires_at_ms IS NULL OR expires_at_ms > issued_at_ms)
                )
                """);
    }

    private static void createIpBansTable(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE IF NOT EXISTS ip_bans (
                    id INTEGER PRIMARY KEY,
                    address TEXT NOT NULL,
                    target_uuid TEXT,
                    target_name TEXT,
                    reason TEXT NOT NULL,
                    issued_at_ms INTEGER NOT NULL,
                    expires_at_ms INTEGER,
                    moderator_uuid TEXT,
                    moderator_name TEXT NOT NULL,
                    state TEXT NOT NULL DEFAULT 'ACTIVE'
                        CHECK (state IN ('ACTIVE', 'EXPIRED', 'REVOKED')),
                    revoked_at_ms INTEGER,
                    revoked_by_uuid TEXT,
                    revoked_by_name TEXT,
                    CHECK (expires_at_ms IS NULL OR expires_at_ms > issued_at_ms),
                    CHECK ((target_uuid IS NULL AND target_name IS NULL)
                        OR (target_uuid IS NOT NULL AND target_name IS NOT NULL))
                )
                """);
    }

    /*
     * Schema creation and the v3.2.1 nullable-expiration upgrade intentionally
     * share the definitions above so fresh and upgraded databases are identical.
     */

    public synchronized List<BanRecord> loadActiveBans(long nowMs) throws SQLException {
        return inTransaction(() -> {
            expireBans(nowMs);
            List<BanRecord> bans = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT id, target_uuid, target_name, reason, issued_at_ms, expires_at_ms,
                           moderator_uuid, moderator_name
                    FROM bans
                    WHERE state = 'ACTIVE'
                      AND (expires_at_ms IS NULL OR expires_at_ms > ?)
                    """)) {
                statement.setLong(1, nowMs);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        bans.add(readBan(result));
                    }
                }
            }
            return bans;
        });
    }

    public synchronized Optional<BanRecord> insertBan(
            UUID targetUuid,
            String targetName,
            String reason,
            long issuedAtMs,
            Long expiresAtMs,
            Moderator moderator) throws SQLException {
        return inTransaction(() -> {
            expireBans(issuedAtMs);
            if (findActivePlayerBan(targetUuid).isPresent()) {
                return Optional.empty();
            }
            return Optional.of(insertBanRow(
                    targetUuid, targetName, reason, issuedAtMs, expiresAtMs, moderator));
        });
    }

    public synchronized boolean revokeBan(
            UUID targetUuid,
            long revokedAtMs,
            Moderator moderator) throws SQLException {
        return inTransaction(() -> {
            expireBans(revokedAtMs);
            Optional<BanRecord> activeBan = findActivePlayerBan(targetUuid);
            if (activeBan.isEmpty()) {
                return false;
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE bans
                    SET state = 'REVOKED', revoked_at_ms = ?,
                        revoked_by_uuid = ?, revoked_by_name = ?
                    WHERE id = ? AND state = 'ACTIVE'
                    """)) {
                statement.setLong(1, revokedAtMs);
                setNullableUuid(statement, 2, moderator.uuid());
                statement.setString(3, moderator.name());
                statement.setLong(4, activeBan.get().id());
                return statement.executeUpdate() == 1;
            }
        });
    }

    public synchronized List<IpBanRecord> loadActiveIpBans(long nowMs) throws SQLException {
        return inTransaction(() -> {
            expireIpBans(nowMs);
            List<IpBanRecord> bans = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT id, address, target_uuid, target_name, reason, issued_at_ms,
                           expires_at_ms, moderator_uuid, moderator_name
                    FROM ip_bans
                    WHERE state = 'ACTIVE'
                      AND (expires_at_ms IS NULL OR expires_at_ms > ?)
                    """)) {
                statement.setLong(1, nowMs);
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        bans.add(readIpBan(result));
                    }
                }
            }
            return bans;
        });
    }

    public synchronized Optional<IpBanRecord> insertIpBan(
            String address,
            UUID targetUuid,
            String targetName,
            String reason,
            long issuedAtMs,
            Long expiresAtMs,
            Moderator moderator) throws SQLException {
        return inTransaction(() -> {
            expireIpBans(issuedAtMs);
            if (findActiveIpBan(address).isPresent()) {
                return Optional.empty();
            }
            return Optional.of(insertIpBanRow(
                    address, targetUuid, targetName, reason,
                    issuedAtMs, expiresAtMs, moderator));
        });
    }

    public synchronized boolean revokeIpBan(
            String address,
            long revokedAtMs,
            Moderator moderator) throws SQLException {
        return inTransaction(() -> {
            expireIpBans(revokedAtMs);
            Optional<IpBanRecord> activeBan = findActiveIpBan(address);
            if (activeBan.isEmpty()) {
                return false;
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE ip_bans
                    SET state = 'REVOKED', revoked_at_ms = ?,
                        revoked_by_uuid = ?, revoked_by_name = ?
                    WHERE id = ? AND state = 'ACTIVE'
                    """)) {
                statement.setLong(1, revokedAtMs);
                setNullableUuid(statement, 2, moderator.uuid());
                statement.setString(3, moderator.name());
                statement.setLong(4, activeBan.get().id());
                return statement.executeUpdate() == 1;
            }
        });
    }

    public synchronized Optional<PlayerIpBanResult> insertPlayerIpBan(
            String address,
            UUID targetUuid,
            String targetName,
            String reason,
            long issuedAtMs,
            Long expiresAtMs,
            Moderator moderator) throws SQLException {
        return inTransaction(() -> {
            expireBans(issuedAtMs);
            expireIpBans(issuedAtMs);
            Optional<BanRecord> existingPlayerBan = findActivePlayerBan(targetUuid);
            Optional<IpBanRecord> existingIpBan = findActiveIpBan(address);
            if (existingPlayerBan.isPresent() && existingIpBan.isPresent()) {
                return Optional.empty();
            }
            BanRecord playerBan = existingPlayerBan.isPresent()
                    ? existingPlayerBan.get()
                    : insertBanRow(
                            targetUuid, targetName, reason, issuedAtMs, expiresAtMs, moderator);
            IpBanRecord ipBan = existingIpBan.isPresent()
                    ? existingIpBan.get()
                    : insertIpBanRow(
                            address, targetUuid, targetName, reason,
                            issuedAtMs, expiresAtMs, moderator);
            return Optional.of(new PlayerIpBanResult(playerBan, ipBan));
        });
    }

    private Optional<BanRecord> findActivePlayerBan(UUID targetUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, target_uuid, target_name, reason, issued_at_ms, expires_at_ms,
                       moderator_uuid, moderator_name
                FROM bans WHERE target_uuid = ? AND state = 'ACTIVE'
                """)) {
            statement.setString(1, targetUuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readBan(result)) : Optional.empty();
            }
        }
    }

    private Optional<IpBanRecord> findActiveIpBan(String address) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, address, target_uuid, target_name, reason, issued_at_ms,
                       expires_at_ms, moderator_uuid, moderator_name
                FROM ip_bans WHERE address = ? AND state = 'ACTIVE'
                """)) {
            statement.setString(1, address);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(readIpBan(result)) : Optional.empty();
            }
        }
    }

    private BanRecord insertBanRow(
            UUID targetUuid,
            String targetName,
            String reason,
            long issuedAtMs,
            Long expiresAtMs,
            Moderator moderator) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO bans(
                    target_uuid, target_name, reason, issued_at_ms, expires_at_ms,
                    moderator_uuid, moderator_name, state
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, targetUuid.toString());
            statement.setString(2, targetName);
            statement.setString(3, reason);
            statement.setLong(4, issuedAtMs);
            setNullableLong(statement, 5, expiresAtMs);
            setNullableUuid(statement, 6, moderator.uuid());
            statement.setString(7, moderator.name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("SQLite did not return the inserted ban ID");
                }
                return new BanRecord(
                        keys.getLong(1), targetUuid, targetName, reason, issuedAtMs,
                        expiresAtMs, moderator.uuid(), moderator.name());
            }
        }
    }

    private IpBanRecord insertIpBanRow(
            String address,
            UUID targetUuid,
            String targetName,
            String reason,
            long issuedAtMs,
            Long expiresAtMs,
            Moderator moderator) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO ip_bans(
                    address, target_uuid, target_name, reason, issued_at_ms, expires_at_ms,
                    moderator_uuid, moderator_name, state
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, address);
            setNullableUuid(statement, 2, targetUuid);
            if (targetName == null) {
                statement.setNull(3, java.sql.Types.VARCHAR);
            } else {
                statement.setString(3, targetName);
            }
            statement.setString(4, reason);
            statement.setLong(5, issuedAtMs);
            setNullableLong(statement, 6, expiresAtMs);
            setNullableUuid(statement, 7, moderator.uuid());
            statement.setString(8, moderator.name());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("SQLite did not return the inserted IP-ban ID");
                }
                return new IpBanRecord(
                        keys.getLong(1), address, targetUuid, targetName, reason,
                        issuedAtMs, expiresAtMs, moderator.uuid(), moderator.name());
            }
        }
    }

    public synchronized void insertKick(
            UUID targetUuid,
            String targetName,
            String reason,
            long kickedAtMs,
            Moderator moderator) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO kicks(
                    target_uuid, target_name, reason, kicked_at_ms, moderator_uuid, moderator_name
                ) VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, targetUuid.toString());
            statement.setString(2, targetName);
            statement.setString(3, reason);
            statement.setLong(4, kickedAtMs);
            setNullableUuid(statement, 5, moderator.uuid());
            statement.setString(6, moderator.name());
            statement.executeUpdate();
        }
    }

    public synchronized AuditPage loadHistory(
            UUID targetUuid,
            AuditFilter filter,
            int limit,
            int offset) throws SQLException {
        if (limit <= 0) {
            throw new IllegalArgumentException("History page size must be positive");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("History offset cannot be negative");
        }

        return inTransaction(() -> {
            expireBans(clock.millis());
            expireIpBans(clock.millis());
            long totalRecords = countHistory(targetUuid, filter);
            return new AuditPage(queryHistory(targetUuid, filter, limit, offset), totalRecords);
        });
    }

    private long countHistory(UUID targetUuid, AuditFilter filter) throws SQLException {
        String sql = switch (filter) {
            case ALL -> """
                    SELECT
                        (SELECT COUNT(*) FROM bans WHERE target_uuid = ?)
                        + (SELECT COUNT(*) FROM kicks WHERE target_uuid = ?)
                        + (SELECT COUNT(*) FROM ip_bans WHERE target_uuid = ?)
                    """;
            case BANS -> """
                    SELECT
                        (SELECT COUNT(*) FROM bans WHERE target_uuid = ?)
                        + (SELECT COUNT(*) FROM ip_bans WHERE target_uuid = ?)
                    """;
            case KICKS -> "SELECT COUNT(*) FROM kicks WHERE target_uuid = ?";
        };
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, targetUuid.toString());
            int targetParameters = switch (filter) {
                case ALL -> 3;
                case BANS -> 2;
                case KICKS -> 1;
            };
            for (int parameter = 2; parameter <= targetParameters; parameter++) {
                statement.setString(parameter, targetUuid.toString());
            }
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : 0;
            }
        }
    }

    private List<AuditRecord> queryHistory(
            UUID targetUuid,
            AuditFilter filter,
            int limit,
            int offset) throws SQLException {
        String sql = switch (filter) {
            case ALL -> """
                    SELECT id, action, target_uuid, target_name, address, reason, occurred_at_ms,
                           expires_at_ms, moderator_uuid, moderator_name, state,
                           revoked_at_ms, revoked_by_uuid, revoked_by_name
                    FROM (
                        SELECT id, 'BAN' AS action, target_uuid, target_name, NULL AS address, reason,
                               issued_at_ms AS occurred_at_ms, expires_at_ms,
                               moderator_uuid, moderator_name, state,
                               revoked_at_ms, revoked_by_uuid, revoked_by_name
                        FROM bans WHERE target_uuid = ?
                        UNION ALL
                        SELECT id, 'KICK' AS action, target_uuid, target_name, NULL AS address, reason,
                               kicked_at_ms AS occurred_at_ms, NULL AS expires_at_ms,
                               moderator_uuid, moderator_name, NULL AS state,
                               NULL AS revoked_at_ms, NULL AS revoked_by_uuid,
                               NULL AS revoked_by_name
                        FROM kicks WHERE target_uuid = ?
                        UNION ALL
                        SELECT id, 'IP_BAN' AS action, target_uuid, target_name, address, reason,
                               issued_at_ms AS occurred_at_ms, expires_at_ms,
                               moderator_uuid, moderator_name, state,
                               revoked_at_ms, revoked_by_uuid, revoked_by_name
                        FROM ip_bans WHERE target_uuid = ?
                    )
                    ORDER BY occurred_at_ms DESC, id DESC, action ASC
                    LIMIT ? OFFSET ?
                    """;
            case BANS -> """
                    SELECT id, action, target_uuid, target_name, address, reason, occurred_at_ms,
                           expires_at_ms, moderator_uuid, moderator_name, state,
                           revoked_at_ms, revoked_by_uuid, revoked_by_name
                    FROM (
                        SELECT id, 'BAN' AS action, target_uuid, target_name,
                               NULL AS address, reason, issued_at_ms AS occurred_at_ms,
                               expires_at_ms, moderator_uuid, moderator_name, state,
                               revoked_at_ms, revoked_by_uuid, revoked_by_name
                        FROM bans WHERE target_uuid = ?
                        UNION ALL
                        SELECT id, 'IP_BAN' AS action, target_uuid, target_name,
                               address, reason, issued_at_ms AS occurred_at_ms,
                               expires_at_ms, moderator_uuid, moderator_name, state,
                               revoked_at_ms, revoked_by_uuid, revoked_by_name
                        FROM ip_bans WHERE target_uuid = ?
                    )
                    ORDER BY occurred_at_ms DESC, id DESC, action ASC
                    LIMIT ? OFFSET ?
                    """;
            case KICKS -> """
                    SELECT id, 'KICK' AS action, target_uuid, target_name, NULL AS address, reason,
                           kicked_at_ms AS occurred_at_ms, NULL AS expires_at_ms,
                           moderator_uuid, moderator_name, NULL AS state,
                           NULL AS revoked_at_ms, NULL AS revoked_by_uuid,
                           NULL AS revoked_by_name
                    FROM kicks
                    WHERE target_uuid = ?
                    ORDER BY occurred_at_ms DESC, id DESC
                    LIMIT ? OFFSET ?
                    """;
        };

        List<AuditRecord> records = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameter = 1;
            statement.setString(parameter++, targetUuid.toString());
            int targetParameters = switch (filter) {
                case ALL -> 3;
                case BANS -> 2;
                case KICKS -> 1;
            };
            while (parameter <= targetParameters) {
                statement.setString(parameter++, targetUuid.toString());
            }
            statement.setInt(parameter++, limit);
            statement.setInt(parameter, offset);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    records.add(readAuditRecord(result));
                }
            }
        }
        return records;
    }

    synchronized long countKicks(UUID targetUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM kicks WHERE target_uuid = ?")) {
            statement.setString(1, targetUuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : 0;
            }
        }
    }

    private void expireBans(long nowMs) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE bans SET state = 'EXPIRED'
                WHERE state = 'ACTIVE' AND expires_at_ms IS NOT NULL
                  AND expires_at_ms <= ?
                """)) {
            statement.setLong(1, nowMs);
            statement.executeUpdate();
        }
    }

    private void expireIpBans(long nowMs) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE ip_bans SET state = 'EXPIRED'
                WHERE state = 'ACTIVE' AND expires_at_ms IS NOT NULL
                  AND expires_at_ms <= ?
                """)) {
            statement.setLong(1, nowMs);
            statement.executeUpdate();
        }
    }

    private static BanRecord readBan(ResultSet result) throws SQLException {
        try {
            String moderator = result.getString("moderator_uuid");
            return new BanRecord(
                    result.getLong("id"),
                    UUID.fromString(result.getString("target_uuid")),
                    result.getString("target_name"),
                    result.getString("reason"),
                    result.getLong("issued_at_ms"),
                    nullableLong(result, "expires_at_ms"),
                    moderator == null ? null : UUID.fromString(moderator),
                    result.getString("moderator_name"));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Database contains an invalid moderation UUID", e);
        }
    }

    private static IpBanRecord readIpBan(ResultSet result) throws SQLException {
        try {
            String target = result.getString("target_uuid");
            String moderator = result.getString("moderator_uuid");
            return new IpBanRecord(
                    result.getLong("id"),
                    result.getString("address"),
                    target == null ? null : UUID.fromString(target),
                    result.getString("target_name"),
                    result.getString("reason"),
                    result.getLong("issued_at_ms"),
                    nullableLong(result, "expires_at_ms"),
                    moderator == null ? null : UUID.fromString(moderator),
                    result.getString("moderator_name"));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Database contains an invalid IP-ban value", e);
        }
    }

    private static AuditRecord readAuditRecord(ResultSet result) throws SQLException {
        try {
            String moderator = result.getString("moderator_uuid");
            long expiration = result.getLong("expires_at_ms");
            Long expiresAtMs = result.wasNull() ? null : expiration;
            long revocation = result.getLong("revoked_at_ms");
            Long revokedAtMs = result.wasNull() ? null : revocation;
            String revokedBy = result.getString("revoked_by_uuid");
            return new AuditRecord(
                    result.getLong("id"),
                    AuditRecord.Action.valueOf(result.getString("action")),
                    UUID.fromString(result.getString("target_uuid")),
                    result.getString("target_name"),
                    result.getString("address"),
                    result.getString("reason"),
                    result.getLong("occurred_at_ms"),
                    expiresAtMs,
                    moderator == null ? null : UUID.fromString(moderator),
                    result.getString("moderator_name"),
                    result.getString("state"),
                    revokedAtMs,
                    revokedBy == null ? null : UUID.fromString(revokedBy),
                    result.getString("revoked_by_name"));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Database contains an invalid moderation history value", e);
        }
    }

    private static void setNullableUuid(PreparedStatement statement, int index, UUID uuid) throws SQLException {
        if (uuid == null) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, uuid.toString());
        }
    }

    private static void setNullableLong(PreparedStatement statement, int index, Long value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private static Long nullableLong(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }

    private <T> T inTransaction(SqlOperation<T> operation) throws SQLException {
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T result = operation.run();
            connection.commit();
            return result;
        } catch (SQLException | RuntimeException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackError) {
                e.addSuppressed(rollbackError);
            }
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    @Override
    public synchronized void close() throws SQLException {
        connection.close();
    }

    @FunctionalInterface
    private interface SqlOperation<T> {
        T run() throws SQLException;
    }
}

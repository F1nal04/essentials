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

/** Owns the single SQLite connection and all moderation schema migrations. */
public final class ModerationDatabase implements AutoCloseable {

    private static final int CURRENT_SCHEMA_VERSION = 1;

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
            database.migrate();
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

    private void migrate() throws SQLException {
        int version = currentSchemaVersion();
        if (version > CURRENT_SCHEMA_VERSION) {
            throw new SQLException("Database schema version " + version
                    + " is newer than this mod supports (" + CURRENT_SCHEMA_VERSION + ")");
        }
        if (version == 0) {
            inTransaction(() -> {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("""
                            CREATE TABLE schema_migrations (
                                version INTEGER PRIMARY KEY,
                                applied_at_ms INTEGER NOT NULL
                            )
                            """);
                    statement.execute("""
                            CREATE TABLE bans (
                                id INTEGER PRIMARY KEY,
                                target_uuid TEXT NOT NULL,
                                target_name TEXT NOT NULL,
                                reason TEXT NOT NULL,
                                issued_at_ms INTEGER NOT NULL,
                                expires_at_ms INTEGER NOT NULL,
                                moderator_uuid TEXT,
                                moderator_name TEXT NOT NULL,
                                state TEXT NOT NULL DEFAULT 'ACTIVE'
                                    CHECK (state IN ('ACTIVE', 'EXPIRED', 'REVOKED')),
                                revoked_at_ms INTEGER,
                                revoked_by_uuid TEXT,
                                revoked_by_name TEXT,
                                CHECK (expires_at_ms > issued_at_ms)
                            )
                            """);
                    statement.execute("""
                            CREATE UNIQUE INDEX bans_one_active_per_player
                            ON bans(target_uuid) WHERE state = 'ACTIVE'
                            """);
                    statement.execute("""
                            CREATE INDEX bans_active_expiration
                            ON bans(state, expires_at_ms)
                            """);
                    statement.execute("""
                            CREATE TABLE kicks (
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
                            CREATE INDEX kicks_target_time
                            ON kicks(target_uuid, kicked_at_ms DESC)
                            """);
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO schema_migrations(version, applied_at_ms) VALUES (?, ?)")) {
                    statement.setInt(1, 1);
                    statement.setLong(2, clock.millis());
                    statement.executeUpdate();
                }
                return null;
            });
        }
    }

    private int currentSchemaVersion() throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet tables = statement.executeQuery("""
                        SELECT 1 FROM sqlite_master
                        WHERE type = 'table' AND name = 'schema_migrations'
                        """)) {
            if (!tables.next()) {
                return 0;
            }
        }
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT COALESCE(MAX(version), 0) FROM schema_migrations")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    public synchronized List<BanRecord> loadActiveBans(long nowMs) throws SQLException {
        return inTransaction(() -> {
            expireBans(nowMs);
            List<BanRecord> bans = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT id, target_uuid, target_name, reason, issued_at_ms, expires_at_ms,
                           moderator_uuid, moderator_name
                    FROM bans
                    WHERE state = 'ACTIVE' AND expires_at_ms > ?
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
            long expiresAtMs,
            Moderator moderator) throws SQLException {
        return inTransaction(() -> {
            expireBans(issuedAtMs);
            try (PreparedStatement existing = connection.prepareStatement(
                    "SELECT 1 FROM bans WHERE target_uuid = ? AND state = 'ACTIVE'")) {
                existing.setString(1, targetUuid.toString());
                try (ResultSet result = existing.executeQuery()) {
                    if (result.next()) {
                        return Optional.empty();
                    }
                }
            }

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
                statement.setLong(5, expiresAtMs);
                setNullableUuid(statement, 6, moderator.uuid());
                statement.setString(7, moderator.name());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("SQLite did not return the inserted ban ID");
                    }
                    return Optional.of(new BanRecord(
                            keys.getLong(1), targetUuid, targetName, reason, issuedAtMs,
                            expiresAtMs, moderator.uuid(), moderator.name()));
                }
            }
        });
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

    synchronized long countKicks(UUID targetUuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM kicks WHERE target_uuid = ?")) {
            statement.setString(1, targetUuid.toString());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong(1) : 0;
            }
        }
    }

    synchronized int schemaVersion() throws SQLException {
        return currentSchemaVersion();
    }

    private void expireBans(long nowMs) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE bans SET state = 'EXPIRED'
                WHERE state = 'ACTIVE' AND expires_at_ms <= ?
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
                    result.getLong("expires_at_ms"),
                    moderator == null ? null : UUID.fromString(moderator),
                    result.getString("moderator_name"));
        } catch (IllegalArgumentException e) {
            throw new SQLException("Database contains an invalid moderation UUID", e);
        }
    }

    private static void setNullableUuid(PreparedStatement statement, int index, UUID uuid) throws SQLException {
        if (uuid == null) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, uuid.toString());
        }
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

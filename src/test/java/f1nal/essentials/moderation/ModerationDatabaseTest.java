package f1nal.essentials.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModerationDatabaseTest {

    private static final Clock CLOCK = Clock.fixed(Instant.ofEpochMilli(1_000_000L), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    @Test
    void persistsBansAndAlwaysRecordsKicks() throws Exception {
        Path path = tempDir.resolve("essentials.db");
        UUID target = UUID.randomUUID();
        Moderator moderator = new Moderator(UUID.randomUUID(), "Mod");

        try (ModerationDatabase database = ModerationDatabase.open(path, CLOCK)) {
            Optional<BanRecord> inserted = database.insertBan(
                    target, "Target", "Reason", 1_000_000L, 2_000_000L, moderator);
            assertTrue(inserted.isPresent());
            assertTrue(database.insertBan(
                    target, "Target", "Other", 1_100_000L, 2_100_000L, moderator).isEmpty());

            database.insertKick(target, "Target", "Spam", 1_200_000L, moderator);
            assertEquals(1, database.countKicks(target));
        }

        try (ModerationDatabase reopened = ModerationDatabase.open(path, CLOCK)) {
            assertEquals(1, reopened.loadActiveBans(1_500_000L).size());
            assertEquals(1, reopened.countKicks(target));
        }
    }

    @Test
    void expiresOldBanBeforeAllowingReplacement() throws Exception {
        UUID target = UUID.randomUUID();
        Moderator moderator = new Moderator(null, "CONSOLE");

        try (ModerationDatabase database = ModerationDatabase.open(
                tempDir.resolve("essentials.db"), CLOCK)) {
            assertTrue(database.insertBan(
                    target, "Target", "Old", 1_000L, 2_000L, moderator).isPresent());
            assertTrue(database.loadActiveBans(2_000L).isEmpty());
            assertTrue(database.insertBan(
                    target, "Target", "New", 2_000L, 3_000L, moderator).isPresent());
        }
    }

    @Test
    void revokesActiveBanAndPreservesAuditDetails() throws Exception {
        UUID target = UUID.randomUUID();
        Moderator issuer = new Moderator(null, "CONSOLE");
        Moderator revoker = new Moderator(UUID.randomUUID(), "Moderator");

        try (ModerationDatabase database = ModerationDatabase.open(
                tempDir.resolve("essentials.db"), CLOCK)) {
            assertTrue(database.insertBan(
                    target, "Target", "Reason",
                    1_000_000L, 2_000_000L, issuer).isPresent());
            assertTrue(database.revokeBan(target, 1_100_000L, revoker));
            assertTrue(database.loadActiveBans(1_100_000L).isEmpty());
            assertFalse(database.revokeBan(target, 1_200_000L, revoker));

            AuditRecord record = database.loadHistory(
                    target, AuditFilter.BANS, 10, 0).records().getFirst();
            assertEquals("REVOKED", record.state());
            assertEquals(1_100_000L, record.revokedAtMs());
            assertEquals(revoker.uuid(), record.revokedByUuid());
            assertEquals("Moderator", record.revokedByName());

            assertTrue(database.insertBan(
                    target, "Target", "Replacement",
                    1_200_000L, 2_200_000L, issuer).isPresent());
        }
    }

    @Test
    void readsCombinedFilteredAndPaginatedHistoryNewestFirst() throws Exception {
        UUID target = UUID.randomUUID();
        Moderator moderator = new Moderator(UUID.randomUUID(), "Mod");

        try (ModerationDatabase database = ModerationDatabase.open(
                tempDir.resolve("essentials.db"), CLOCK)) {
            database.insertBan(target, "Target", "First ban", 1_000L, 2_000L, moderator);
            database.insertKick(target, "Target", "First kick", 2_500L, moderator);
            database.insertKick(target, "Target", "Second kick", 3_000L, moderator);
            database.insertBan(target, "Target", "Second ban", 3_500L, 5_500L, moderator);

            AuditPage firstPage = database.loadHistory(target, AuditFilter.ALL, 2, 0);
            assertEquals(4, firstPage.totalRecords());
            assertEquals(2, firstPage.records().size());
            assertEquals("Second ban", firstPage.records().get(0).reason());
            assertEquals(AuditRecord.Action.BAN, firstPage.records().get(0).action());
            assertEquals("EXPIRED", firstPage.records().get(0).state());
            assertEquals("Second kick", firstPage.records().get(1).reason());

            AuditPage secondPage = database.loadHistory(target, AuditFilter.ALL, 2, 2);
            assertEquals(4, secondPage.totalRecords());
            assertEquals("First kick", secondPage.records().get(0).reason());
            assertEquals("First ban", secondPage.records().get(1).reason());

            AuditPage bans = database.loadHistory(target, AuditFilter.BANS, 10, 0);
            assertEquals(2, bans.totalRecords());
            assertTrue(bans.records().stream()
                    .allMatch(record -> record.action() == AuditRecord.Action.BAN));

            AuditPage kicks = database.loadHistory(target, AuditFilter.KICKS, 10, 0);
            assertEquals(2, kicks.totalRecords());
            assertTrue(kicks.records().stream()
                    .allMatch(record -> record.action() == AuditRecord.Action.KICK));
        }
    }

    @Test
    void persistsExpiresAndAuditsPlayerAssociatedIpBans() throws Exception {
        UUID target = UUID.randomUUID();
        Moderator moderator = new Moderator(UUID.randomUUID(), "Mod");

        try (ModerationDatabase database = ModerationDatabase.open(
                tempDir.resolve("essentials.db"), CLOCK)) {
            Optional<IpBanRecord> inserted = database.insertIpBan(
                    "2001:db8::1", target, "Target", "Proxy", 1_000L, 2_000L, moderator);
            assertTrue(inserted.isPresent());
            assertTrue(database.insertIpBan(
                    "2001:db8::1", null, null, "Other", 1_100L, 2_100L, moderator).isEmpty());
            assertEquals(1, database.loadActiveIpBans(1_500L).size());
            assertTrue(database.loadActiveIpBans(2_000L).isEmpty());
            assertTrue(database.insertIpBan(
                    "2001:db8::1", null, null, "Replacement", 2_000L, 3_000L, moderator)
                    .isPresent());

            AuditPage history = database.loadHistory(target, AuditFilter.BANS, 10, 0);
            assertEquals(1, history.totalRecords());
            assertEquals(AuditRecord.Action.IP_BAN, history.records().getFirst().action());
            assertEquals("2001:db8::1", history.records().getFirst().address());
            assertEquals("EXPIRED", history.records().getFirst().state());
        }
    }

    @Test
    void revokesActiveIpBanAndPreservesAuditDetails() throws Exception {
        UUID target = UUID.randomUUID();
        Moderator issuer = new Moderator(null, "CONSOLE");
        Moderator revoker = new Moderator(UUID.randomUUID(), "Moderator");

        try (ModerationDatabase database = ModerationDatabase.open(
                tempDir.resolve("essentials.db"), CLOCK)) {
            assertTrue(database.insertIpBan(
                    "2001:db8::20", target, "Target", "Proxy",
                    1_000_000L, 2_000_000L, issuer).isPresent());
            assertTrue(database.revokeIpBan("2001:db8::20", 1_100_000L, revoker));
            assertTrue(database.loadActiveIpBans(1_100_000L).isEmpty());
            assertFalse(database.revokeIpBan("2001:db8::20", 1_200_000L, revoker));

            AuditRecord record = database.loadHistory(
                    target, AuditFilter.BANS, 10, 0).records().getFirst();
            assertEquals(AuditRecord.Action.IP_BAN, record.action());
            assertEquals("REVOKED", record.state());
            assertEquals(1_100_000L, record.revokedAtMs());
            assertEquals(revoker.uuid(), record.revokedByUuid());
            assertEquals("Moderator", record.revokedByName());
        }
    }

    @Test
    void atomicallyBansPlayerAccountAndIpAddress() throws Exception {
        UUID target = UUID.randomUUID();
        UUID otherTarget = UUID.randomUUID();
        Moderator moderator = new Moderator(null, "CONSOLE");

        try (ModerationDatabase database = ModerationDatabase.open(
                tempDir.resolve("essentials.db"), CLOCK)) {
            Optional<PlayerIpBanResult> inserted = database.insertPlayerIpBan(
                    "192.0.2.20", target, "Target", "Evasion",
                    1_000_000L, 2_000_000L, moderator);
            assertTrue(inserted.isPresent());
            assertEquals(target, inserted.get().playerBan().targetUuid());
            assertEquals("192.0.2.20", inserted.get().ipBan().address());
            assertEquals(1, database.loadActiveBans(1_500_000L).size());
            assertEquals(1, database.loadActiveIpBans(1_500_000L).size());

            Optional<PlayerIpBanResult> sharedAddress = database.insertPlayerIpBan(
                    "192.0.2.20", otherTarget, "Other", "Other reason",
                    1_100_000L, 2_100_000L, moderator);
            assertTrue(sharedAddress.isPresent());
            assertEquals(target, sharedAddress.get().ipBan().targetUuid());
            assertEquals(1, database.loadHistory(
                    otherTarget, AuditFilter.BANS, 10, 0).totalRecords());
            assertEquals(2, database.loadActiveBans(1_500_000L).size());
            assertEquals(1, database.loadActiveIpBans(1_500_000L).size());

            assertTrue(database.insertPlayerIpBan(
                    "192.0.2.20", target, "Target", "Duplicate",
                    1_150_000L, 2_150_000L, moderator).isEmpty());

            AuditPage history = database.loadHistory(target, AuditFilter.BANS, 10, 0);
            assertEquals(2, history.totalRecords());
            assertTrue(history.records().stream()
                    .anyMatch(record -> record.action() == AuditRecord.Action.BAN));
            assertTrue(history.records().stream()
                    .anyMatch(record -> record.action() == AuditRecord.Action.IP_BAN));
        }
    }

    @Test
    void persistsPermanentPlayerAndIpBansUntilRevoked() throws Exception {
        Path path = tempDir.resolve("essentials.db");
        UUID target = UUID.randomUUID();
        Moderator moderator = new Moderator(null, "CONSOLE");

        try (ModerationDatabase database = ModerationDatabase.open(path, CLOCK)) {
            PlayerIpBanResult result = database.insertPlayerIpBan(
                    "192.0.2.60", target, "Target", "Permanent",
                    1_000_000L, null, moderator).orElseThrow();
            assertTrue(result.playerBan().permanent());
            assertTrue(result.ipBan().permanent());
        }

        try (ModerationDatabase reopened = ModerationDatabase.open(path, CLOCK)) {
            assertTrue(reopened.loadActiveBans(Long.MAX_VALUE).getFirst().permanent());
            assertTrue(reopened.loadActiveIpBans(Long.MAX_VALUE).getFirst().permanent());
            assertTrue(reopened.revokeBan(target, 2_000_000L, moderator));
            assertTrue(reopened.revokeIpBan("192.0.2.60", 2_000_000L, moderator));
        }
    }

    @Test
    void upgradesReleasedSchemaWithoutLosingFiniteBans() throws Exception {
        Path path = tempDir.resolve("essentials.db");
        UUID target = UUID.randomUUID();
        UUID moderator = UUID.randomUUID();
        createReleasedSchema(path, target, moderator);

        try (ModerationDatabase database = ModerationDatabase.open(path, CLOCK)) {
            BanRecord playerBan = database.loadActiveBans(1_500_000L).getFirst();
            IpBanRecord ipBan = database.loadActiveIpBans(1_500_000L).getFirst();
            assertEquals(target, playerBan.targetUuid());
            assertEquals(2_000_000L, playerBan.expiresAtMs());
            assertEquals("192.0.2.70", ipBan.address());
            assertEquals(2_000_000L, ipBan.expiresAtMs());

            assertTrue(database.insertBan(
                    UUID.randomUUID(), "Permanent", "No expiry",
                    1_500_000L, null, new Moderator(null, "CONSOLE")).isPresent());
            assertTrue(database.insertIpBan(
                    "192.0.2.71", null, null, "No expiry",
                    1_500_000L, null, new Moderator(null, "CONSOLE")).isPresent());
        }

        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath())) {
            assertEquals(0, expirationNotNull(connection, "bans"));
            assertEquals(0, expirationNotNull(connection, "ip_bans"));
        }
    }

    @Test
    void initializesCurrentSchemaWithoutMigrationTable() throws Exception {
        Path path = tempDir.resolve("essentials.db");
        try (ModerationDatabase ignored = ModerationDatabase.open(path, CLOCK)) {
        }
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
                var statement = connection.prepareStatement("""
                        SELECT COUNT(*) FROM sqlite_master
                        WHERE type = 'table' AND name = 'schema_migrations'
                        """);
                var result = statement.executeQuery()) {
            assertTrue(result.next());
            assertEquals(0, result.getInt(1));
        }
    }

    @Test
    void persistsWarningsMutesRevocationsAndPrivateNotes() throws Exception {
        Path path = tempDir.resolve("essentials.db");
        UUID target = UUID.randomUUID();
        Moderator issuer = new Moderator(UUID.randomUUID(), "Mod");
        Moderator revoker = new Moderator(null, "CONSOLE");

        try (ModerationDatabase database = ModerationDatabase.open(path, CLOCK)) {
            database.insertWarning(target, "Target", "First", 900_000L, issuer);
            database.insertWarning(target, "Target", "Second", 990_000L, issuer);
            assertEquals(1, database.countWarningsSince(target, 950_000L));

            assertTrue(database.insertMute(
                    target, "Target", "Spam", 1_000_000L, null, issuer).isPresent());
            assertTrue(database.insertMute(
                    target, "Target", "Duplicate", 1_000_001L, null, issuer).isEmpty());
            database.insertStaffNote(target, "Target", "Internal context", 1_010_000L, issuer);
            assertTrue(database.revokeMute(target, 1_020_000L, revoker));

            assertEquals(2, database.loadHistory(
                    target, AuditFilter.WARNINGS, 10, 0).totalRecords());
            AuditRecord mute = database.loadHistory(
                    target, AuditFilter.MUTES, 10, 0).records().getFirst();
            assertEquals("REVOKED", mute.state());
            assertEquals("CONSOLE", mute.revokedByName());
            AuditRecord note = database.loadHistory(
                    target, AuditFilter.NOTES, 10, 0).records().getFirst();
            assertEquals(AuditRecord.Action.NOTE, note.action());
            assertEquals("Internal context", note.reason());
            assertEquals(4, database.loadHistory(
                    target, AuditFilter.ALL, 10, 0).totalRecords());
        }

        try (ModerationDatabase reopened = ModerationDatabase.open(path, CLOCK)) {
            assertTrue(reopened.loadActiveMutes(CLOCK.millis()).isEmpty());
            assertEquals(4, reopened.loadHistory(
                    target, AuditFilter.ALL, 10, 0).totalRecords());
        }
    }

    private static void createReleasedSchema(Path path, UUID target, UUID moderator)
            throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
                var statement = connection.createStatement()) {
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
                        state TEXT NOT NULL DEFAULT 'ACTIVE',
                        revoked_at_ms INTEGER,
                        revoked_by_uuid TEXT,
                        revoked_by_name TEXT,
                        CHECK (expires_at_ms > issued_at_ms)
                    )
                    """);
            statement.execute("""
                    CREATE TABLE ip_bans (
                        id INTEGER PRIMARY KEY,
                        address TEXT NOT NULL,
                        target_uuid TEXT,
                        target_name TEXT,
                        reason TEXT NOT NULL,
                        issued_at_ms INTEGER NOT NULL,
                        expires_at_ms INTEGER NOT NULL,
                        moderator_uuid TEXT,
                        moderator_name TEXT NOT NULL,
                        state TEXT NOT NULL DEFAULT 'ACTIVE',
                        revoked_at_ms INTEGER,
                        revoked_by_uuid TEXT,
                        revoked_by_name TEXT,
                        CHECK (expires_at_ms > issued_at_ms)
                    )
                    """);
            try (var insert = connection.prepareStatement("""
                    INSERT INTO bans(
                        target_uuid, target_name, reason, issued_at_ms, expires_at_ms,
                        moderator_uuid, moderator_name, state
                    ) VALUES (?, 'Target', 'Finite', 1000000, 2000000, ?, 'Moderator', 'ACTIVE')
                    """)) {
                insert.setString(1, target.toString());
                insert.setString(2, moderator.toString());
                insert.executeUpdate();
            }
            try (var insert = connection.prepareStatement("""
                    INSERT INTO ip_bans(
                        address, target_uuid, target_name, reason, issued_at_ms, expires_at_ms,
                        moderator_uuid, moderator_name, state
                    ) VALUES ('192.0.2.70', ?, 'Target', 'Finite', 1000000, 2000000,
                              ?, 'Moderator', 'ACTIVE')
                    """)) {
                insert.setString(1, target.toString());
                insert.setString(2, moderator.toString());
                insert.executeUpdate();
            }
        }
    }

    private static int expirationNotNull(java.sql.Connection connection, String table)
            throws Exception {
        try (var statement = connection.createStatement();
                var result = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (result.next()) {
                if ("expires_at_ms".equals(result.getString("name"))) {
                    return result.getInt("notnull");
                }
            }
            throw new AssertionError("Missing expires_at_ms in " + table);
        }
    }
}

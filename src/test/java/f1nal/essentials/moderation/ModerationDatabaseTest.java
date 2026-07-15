package f1nal.essentials.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void migratesPersistsBansAndAlwaysRecordsKicks() throws Exception {
        Path path = tempDir.resolve("essentials.db");
        UUID target = UUID.randomUUID();
        Moderator moderator = new Moderator(UUID.randomUUID(), "Mod");

        try (ModerationDatabase database = ModerationDatabase.open(path, CLOCK)) {
            assertEquals(2, database.schemaVersion());
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
    void upgradesExistingVersionOneDatabaseForHistoryQueries() throws Exception {
        Path path = tempDir.resolve("essentials.db");
        try (ModerationDatabase ignored = ModerationDatabase.open(path, CLOCK)) {
            // Create the current database first, then faithfully simulate the
            // schema state written by releases that stopped at migration 1.
        }
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
                var statement = connection.createStatement()) {
            statement.execute("DROP INDEX bans_target_time");
            statement.execute("DELETE FROM schema_migrations WHERE version = 2");
        }

        try (ModerationDatabase upgraded = ModerationDatabase.open(path, CLOCK)) {
            assertEquals(2, upgraded.schemaVersion());
        }
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
                var statement = connection.prepareStatement("""
                        SELECT COUNT(*) FROM sqlite_master
                        WHERE type = 'index' AND name = 'bans_target_time'
                        """);
                var result = statement.executeQuery()) {
            assertTrue(result.next());
            assertEquals(1, result.getInt(1));
        }
    }
}

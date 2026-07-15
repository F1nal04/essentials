package f1nal.essentials.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
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
            assertEquals(1, database.schemaVersion());
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
}

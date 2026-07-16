package f1nal.essentials.moderation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModerationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void activeBanCacheExpiresWithoutDatabaseIo() throws Exception {
        MutableClock clock = new MutableClock(10_000L);
        UUID target = UUID.randomUUID();

        try (ModerationService service = ModerationService.open(
                tempDir.resolve("essentials.db"), clock)) {
            assertTrue(service.ban(
                    target, "Target", "Reason", 1_000L, new Moderator(null, "CONSOLE"))
                    .isPresent());
            assertTrue(service.activeBan(target).isPresent());

            clock.setMillis(11_000L);
            assertTrue(service.activeBan(target).isEmpty());
        }
    }

    @Test
    void combinedPlayerAndIpBanCachesExpireTogetherWithoutDatabaseIo() throws Exception {
        MutableClock clock = new MutableClock(20_000L);
        UUID target = UUID.randomUUID();

        try (ModerationService service = ModerationService.open(
                tempDir.resolve("essentials.db"), clock)) {
            assertTrue(service.banPlayerIp(
                    "192.0.2.10", target, "Target", "Reason", 1_000L,
                    new Moderator(null, "CONSOLE")).isPresent());
            assertTrue(service.activeBan(target).isPresent());
            assertTrue(service.activeIpBan("192.0.2.10").isPresent());
            assertTrue(service.activeIpBans(target).size() == 1);

            clock.setMillis(21_000L);
            assertTrue(service.activeBan(target).isEmpty());
            assertTrue(service.activeIpBan("192.0.2.10").isEmpty());
            assertTrue(service.activeIpBans(target).isEmpty());
        }
    }

    @Test
    void pardonRevokesPersistedBanAndClearsActiveCache() throws Exception {
        MutableClock clock = new MutableClock(30_000L);
        UUID target = UUID.randomUUID();
        Moderator moderator = new Moderator(null, "CONSOLE");

        try (ModerationService service = ModerationService.open(
                tempDir.resolve("essentials.db"), clock)) {
            assertTrue(service.ban(
                    target, "Target", "Reason", 10_000L, moderator).isPresent());
            assertTrue(service.pardon(target, moderator));
            assertTrue(service.activeBan(target).isEmpty());
            assertFalse(service.pardon(target, moderator));
        }
    }

    @Test
    void pardonIpRevokesOnlyIpBanAndClearsItsCache() throws Exception {
        MutableClock clock = new MutableClock(40_000L);
        UUID target = UUID.randomUUID();
        Moderator moderator = new Moderator(null, "CONSOLE");

        try (ModerationService service = ModerationService.open(
                tempDir.resolve("essentials.db"), clock)) {
            assertTrue(service.banPlayerIp(
                    "2001:db8::30", target, "Target", "Proxy", 10_000L, moderator)
                    .isPresent());
            assertTrue(service.pardonIp("2001:0db8:0:0:0:0:0:30", moderator));
            assertTrue(service.activeIpBan("2001:db8::30").isEmpty());
            assertTrue(service.activeBan(target).isPresent());
            assertFalse(service.pardonIp("2001:db8::30", moderator));
        }
    }

    private static final class MutableClock extends Clock {
        private long millis;

        private MutableClock(long millis) {
            this.millis = millis;
        }

        void setMillis(long millis) {
            this.millis = millis;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }

        @Override
        public long millis() {
            return millis;
        }
    }
}

package f1nal.essentials.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class ModerationMessageFormatterTest {

    @Test
    void rendersBanPlaceholders() {
        BanRecord ban = new BanRecord(
                1, UUID.randomUUID(), "Target", "Griefing", 1_000, 3_601_000L,
                UUID.randomUUID(), "Moderator");

        assertEquals(
                "Target|Griefing|Moderator|1h|1970-01-01T01:00:01Z",
                ModerationMessageFormatter.banMessage(
                        "{player}|{reason}|{moderator}|{time}|{expires_at}", ban, 1_000));
    }

    @Test
    void rendersKickPlaceholders() {
        assertEquals(
                "Target|Spam|CONSOLE",
                ModerationMessageFormatter.kickMessage(
                        "{player}|{reason}|{moderator}", "Target", "Spam", "CONSOLE"));
    }

    @Test
    void rendersIpBanPlaceholders() {
        IpBanRecord ban = new IpBanRecord(
                2, "192.0.2.10", null, null, "Proxy", 1_000, 3_601_000L,
                null, "CONSOLE");

        assertEquals(
                "192.0.2.10|Proxy|CONSOLE|1h|1970-01-01T01:00:01Z",
                ModerationMessageFormatter.ipBanMessage(
                        "{player}|{reason}|{moderator}|{time}|{expires_at}", ban, 1_000));
    }

    @Test
    void rendersPermanentBanPlaceholders() {
        BanRecord ban = new BanRecord(
                3, UUID.randomUUID(), "Target", "Permanent reason", 1_000, null,
                null, "CONSOLE");

        assertEquals(
                "Permanent|Never",
                ModerationMessageFormatter.banMessage(
                        "{time}|{expires_at}", ban, Long.MAX_VALUE));
    }

    @Test
    void doesNotInterpretPlaceholdersInsideReasons() {
        assertEquals(
                "Reason: literal {moderator}",
                ModerationMessageFormatter.kickMessage(
                        "Reason: {reason}", "Target", "literal {moderator}", "CONSOLE"));
    }
}

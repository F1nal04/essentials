package f1nal.essentials.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class ModerationMessageFormatterTest {

    @Test
    void rendersBanPlaceholders() {
        BanRecord ban = new BanRecord(
                1, UUID.randomUUID(), "Target", "Griefing", 1_000, 3_601_000,
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
    void doesNotInterpretPlaceholdersInsideReasons() {
        assertEquals(
                "Reason: literal {moderator}",
                ModerationMessageFormatter.kickMessage(
                        "Reason: {reason}", "Target", "literal {moderator}", "CONSOLE"));
    }
}

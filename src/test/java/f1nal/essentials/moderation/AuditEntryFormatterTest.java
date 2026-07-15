package f1nal.essentials.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class AuditEntryFormatterTest {

    private static final UUID TARGET = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void formatsEveryRequestedBanField() {
        AuditRecord ban = new AuditRecord(
                1,
                AuditRecord.Action.BAN,
                TARGET,
                "Target",
                "Griefing",
                0,
                5_400_000L,
                null,
                "CONSOLE",
                "EXPIRED");

        assertEquals(
                "[BAN] When: 1970-01-01 00:00:00 Z | Duration: 1h 30m"
                        + " | By: CONSOLE | Reason: Griefing | Status: expired",
                AuditEntryFormatter.format(ban, ZoneOffset.UTC));
    }

    @Test
    void explainsThatKicksHaveNoDuration() {
        AuditRecord kick = new AuditRecord(
                2,
                AuditRecord.Action.KICK,
                TARGET,
                "Target",
                "Spam",
                60_000,
                null,
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "Moderator",
                null);

        assertEquals(
                "[KICK] When: 1970-01-01 00:01:00 Z | Duration: n/a (instant action)"
                        + " | By: Moderator | Reason: Spam",
                AuditEntryFormatter.format(kick, ZoneOffset.UTC));
    }
}

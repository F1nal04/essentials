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
                null,
                "Griefing",
                0,
                5_400_000L,
                null,
                "CONSOLE",
                "EXPIRED");

        assertEquals(
                "[BAN] When: 01/01/1970 00:00:00 Z | Duration: 1h 30m"
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
                null,
                "Spam",
                60_000,
                null,
                UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "Moderator",
                null);

        assertEquals(
                "[KICK] When: 01/01/1970 00:01:00 Z | Duration: n/a (instant action)"
                        + " | By: Moderator | Reason: Spam",
                AuditEntryFormatter.format(kick, ZoneOffset.UTC));
    }

    @Test
    void formatsActiveBanBanner() {
        BanRecord ban = new BanRecord(
                3,
                TARGET,
                "Target",
                "Active reason",
                0,
                5_400_000L,
                null,
                "CONSOLE");

        assertEquals(
                "ACTIVE BAN | Remaining: 1h 30m | Expires: 01/01/1970 01:30:00 Z"
                        + " | By: CONSOLE | Reason: Active reason",
                AuditEntryFormatter.formatActiveBan(ban, 0, ZoneOffset.UTC).getString());
    }

    @Test
    void formatsPlayerAssociatedIpBan() {
        AuditRecord ban = new AuditRecord(
                4,
                AuditRecord.Action.IP_BAN,
                TARGET,
                "Target",
                "2001:db8::1",
                "Proxy abuse",
                0,
                3_600_000L,
                null,
                "CONSOLE",
                "ACTIVE");

        assertEquals(
                "[IP BAN] When: 01/01/1970 00:00:00 Z | Duration: 1h"
                        + " | By: CONSOLE | Reason: Proxy abuse | Address: 2001:db8::1"
                        + " | Status: active",
                AuditEntryFormatter.format(ban, ZoneOffset.UTC));
    }

    @Test
    void formatsActiveIpBanBanner() {
        IpBanRecord ban = new IpBanRecord(
                5,
                "192.0.2.10",
                TARGET,
                "Target",
                "Active proxy",
                0,
                5_400_000L,
                null,
                "CONSOLE");

        assertEquals(
                "ACTIVE IP BAN | Address: 192.0.2.10 | Remaining: 1h 30m"
                        + " | Expires: 01/01/1970 01:30:00 Z"
                        + " | By: CONSOLE | Reason: Active proxy",
                AuditEntryFormatter.formatActiveIpBan(ban, 0, ZoneOffset.UTC).getString());
    }
}

package f1nal.essentials.moderation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class AuditEntryFormatter {

    private static final String KICK_DURATION = "n/a (instant action)";

    private AuditEntryFormatter() {
    }

    public static String format(AuditRecord record, ZoneId zone) {
        DateTimeFormatter timestamp = DateTimeFormatter
                .ofPattern("uuuu-MM-dd HH:mm:ss z", Locale.ROOT)
                .withZone(zone);
        String duration = record.action() == AuditRecord.Action.BAN
                ? DurationParser.formatDuration(record.durationMs())
                : KICK_DURATION;
        String status = record.state() == null
                ? ""
                : " | Status: " + record.state().toLowerCase(Locale.ROOT);
        return "[" + record.action() + "]"
                + " When: " + timestamp.format(Instant.ofEpochMilli(record.occurredAtMs()))
                + " | Duration: " + duration
                + " | By: " + record.moderatorName()
                + " | Reason: " + record.reason()
                + status;
    }
}

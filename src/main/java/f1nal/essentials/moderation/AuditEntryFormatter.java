package f1nal.essentials.moderation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class AuditEntryFormatter {

    private static final String KICK_DURATION = "n/a (instant action)";

    private AuditEntryFormatter() {
    }

    public static String format(AuditRecord record, ZoneId zone) {
        return formatComponent(record, zone).getString();
    }

    public static Component formatComponent(AuditRecord record, ZoneId zone) {
        DateTimeFormatter timestamp = DateTimeFormatter
                .ofPattern("dd/MM/uuuu HH:mm:ss z", Locale.ROOT)
                .withZone(zone);
        String duration = record.action() == AuditRecord.Action.KICK
                ? KICK_DURATION
                : record.expiresAtMs() == null
                        ? "Permanent"
                        : DurationParser.formatDuration(record.durationMs());
        ChatFormatting actionColor = record.action() == AuditRecord.Action.KICK
                ? ChatFormatting.GOLD
                : ChatFormatting.DARK_RED;
        String actionLabel = record.action() == AuditRecord.Action.IP_BAN
                ? "IP BAN"
                : record.action().name();
        MutableComponent line = Component.literal("[" + actionLabel + "]")
                .withStyle(actionColor)
                .append(label(" When: "))
                .append(value(timestamp.format(Instant.ofEpochMilli(record.occurredAtMs()))))
                .append(label(" | Duration: "))
                .append(value(duration))
                .append(label(" | By: "))
                .append(value(record.moderatorName()))
                .append(label(" | Reason: "))
                .append(value(record.reason()));
        if (record.address() != null) {
            line.append(label(" | Address: "))
                    .append(value(record.address()));
        }
        if (record.state() != null) {
            line.append(label(" | Status: "))
                    .append(Component.literal(record.state().toLowerCase(Locale.ROOT))
                            .withStyle(statusColor(record.state())));
        }
        if (record.revokedAtMs() != null) {
            line.append(label(" | Revoked: "))
                    .append(value(timestamp.format(Instant.ofEpochMilli(record.revokedAtMs()))))
                    .append(label(" | Revoked by: "))
                    .append(value(record.revokedByName()));
        }
        return line;
    }

    public static Component formatActiveBan(BanRecord ban, long nowMs, ZoneId zone) {
        DateTimeFormatter timestamp = DateTimeFormatter
                .ofPattern("dd/MM/uuuu HH:mm:ss z", Locale.ROOT)
                .withZone(zone);
        MutableComponent line = Component.literal("ACTIVE BAN")
                .withStyle(ChatFormatting.DARK_GREEN);
        appendExpiration(line, ban.expiresAtMs(), nowMs, timestamp);
        return line.append(label(" | By: "))
                .append(value(ban.moderatorName()))
                .append(label(" | Reason: "))
                .append(value(ban.reason()));
    }

    public static Component formatActiveIpBan(IpBanRecord ban, long nowMs, ZoneId zone) {
        DateTimeFormatter timestamp = DateTimeFormatter
                .ofPattern("dd/MM/uuuu HH:mm:ss z", Locale.ROOT)
                .withZone(zone);
        MutableComponent line = Component.literal("ACTIVE IP BAN")
                .withStyle(ChatFormatting.DARK_GREEN)
                .append(label(" | Address: "))
                .append(value(ban.address()));
        appendExpiration(line, ban.expiresAtMs(), nowMs, timestamp);
        return line.append(label(" | By: "))
                .append(value(ban.moderatorName()))
                .append(label(" | Reason: "))
                .append(value(ban.reason()));
    }

    private static void appendExpiration(
            MutableComponent line,
            Long expiresAtMs,
            long nowMs,
            DateTimeFormatter timestamp) {
        if (expiresAtMs == null) {
            line.append(label(" | Duration: "))
                    .append(value("Permanent"));
            return;
        }
        line.append(label(" | Remaining: "))
                .append(value(DurationParser.formatRemaining(expiresAtMs - nowMs)))
                .append(label(" | Expires: "))
                .append(value(timestamp.format(Instant.ofEpochMilli(expiresAtMs))));
    }

    private static MutableComponent label(String text) {
        return Component.literal(text).withStyle(ChatFormatting.DARK_GRAY);
    }

    private static MutableComponent value(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }

    private static ChatFormatting statusColor(String state) {
        return "ACTIVE".equals(state) ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_GRAY;
    }
}

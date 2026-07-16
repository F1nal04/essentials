package f1nal.essentials.moderation;

import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ModerationMessageFormatter {

    private static final Pattern PLACEHOLDER = Pattern.compile(
            "\\{(player|reason|moderator|time|expires_at)}");

    private ModerationMessageFormatter() {
    }

    public static String banMessage(String template, BanRecord ban, long nowMs) {
        return render(template, Map.of(
                "player", ban.targetName(),
                "reason", ban.reason(),
                "moderator", ban.moderatorName(),
                "time", remaining(ban.expiresAtMs(), nowMs),
                "expires_at", expiration(ban.expiresAtMs())));
    }

    public static String ipBanMessage(String template, IpBanRecord ban, long nowMs) {
        return render(template, Map.of(
                "player", ban.targetDisplay(),
                "reason", ban.reason(),
                "moderator", ban.moderatorName(),
                "time", remaining(ban.expiresAtMs(), nowMs),
                "expires_at", expiration(ban.expiresAtMs())));
    }

    public static String kickMessage(String template, String targetName, String reason, String moderatorName) {
        return render(template, Map.of(
                "player", targetName,
                "reason", reason,
                "moderator", moderatorName));
    }

    private static String render(String template, Map<String, String> values) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder output = new StringBuilder();
        while (matcher.find()) {
            String replacement = values.get(matcher.group(1));
            if (replacement == null) {
                replacement = matcher.group();
            }
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String remaining(Long expiresAtMs, long nowMs) {
        return expiresAtMs == null
                ? "Permanent"
                : DurationParser.formatRemaining(expiresAtMs - nowMs);
    }

    private static String expiration(Long expiresAtMs) {
        return expiresAtMs == null ? "Never" : Instant.ofEpochMilli(expiresAtMs).toString();
    }
}

package f1nal.essentials.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import f1nal.essentials.Essentials;
import f1nal.essentials.moderation.DurationParser;

public final class ModerationConfig {

    private static final String DEFAULT_BAN_MESSAGE =
            "&cYou are banned from this server.\n&7Reason: &f{reason}\n&7Time remaining: &f{time}";
    private static final String DEFAULT_KICK_MESSAGE =
            "&cYou were kicked from this server.\n&7Reason: &f{reason}";
    private static final String DEFAULT_WARNING_MESSAGE =
            "&eYou were warned by {moderator}: &f{reason}";
    private static final String DEFAULT_MUTE_MESSAGE =
            "&cYou were muted by {moderator}. &7Reason: &f{reason} &7Duration: &f{time}";
    private static final String DEFAULT_UNMUTE_MESSAGE = "&aYour mute was revoked by {moderator}.";
    private static final String DEFAULT_MUTE_BLOCKED_MESSAGE =
            "&cYou are muted. &7Reason: &f{reason} &7Time remaining: &f{time}";
    private static final String DEFAULT_MUTE_EXPIRED_MESSAGE = "&aYour mute has expired.";

    private static ModerationConfig instance;

    public final String banMessage;
    public final String kickMessage;
    public final String warningMessage;
    public final String muteMessage;
    public final String unmuteMessage;
    public final String muteBlockedMessage;
    public final String muteExpiredMessage;
    public final boolean muteBlocksPrivateMessages;
    public final long warningRollingPeriodMs;
    public final int warningAlertThreshold;

    private ModerationConfig(
            String banMessage,
            String kickMessage,
            String warningMessage,
            String muteMessage,
            String unmuteMessage,
            String muteBlockedMessage,
            String muteExpiredMessage,
            boolean muteBlocksPrivateMessages,
            long warningRollingPeriodMs,
            int warningAlertThreshold) {
        this.banMessage = banMessage;
        this.kickMessage = kickMessage;
        this.warningMessage = warningMessage;
        this.muteMessage = muteMessage;
        this.unmuteMessage = unmuteMessage;
        this.muteBlockedMessage = muteBlockedMessage;
        this.muteExpiredMessage = muteExpiredMessage;
        this.muteBlocksPrivateMessages = muteBlocksPrivateMessages;
        this.warningRollingPeriodMs = warningRollingPeriodMs;
        this.warningAlertThreshold = warningAlertThreshold;
    }

    public static synchronized ModerationConfig get() {
        if (instance == null) {
            instance = loadOrDefaults();
        }
        return instance;
    }

    static ModerationConfig loadOrDefaults() {
        Path cfg = ConfigPaths.configFile();
        if (!Files.exists(cfg)) {
            return defaults();
        }
        try {
            return parse(Files.readString(cfg, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Essentials.LOGGER.warn(
                    "Failed to read moderation settings from essentials.yaml, using defaults: {}", e.toString());
            return defaults();
        }
    }

    static ModerationConfig parse(String yamlText) {
        try {
            Object root = new Yaml(new LoaderOptions()).load(yamlText);
            if (!(root instanceof Map<?, ?> map)) {
                return defaults();
            }
            Object moderationObj = map.get("moderation");
            if (!(moderationObj instanceof Map<?, ?> moderation)) {
                return defaults();
            }
            String banMessage = moderation.get("ban_message") instanceof String value
                    ? value : DEFAULT_BAN_MESSAGE;
            String kickMessage = moderation.get("kick_message") instanceof String value
                    ? value : DEFAULT_KICK_MESSAGE;
            String warningMessage = stringValue(
                    moderation, "warning_message", DEFAULT_WARNING_MESSAGE);
            String muteMessage = stringValue(moderation, "mute_message", DEFAULT_MUTE_MESSAGE);
            String unmuteMessage = stringValue(
                    moderation, "unmute_message", DEFAULT_UNMUTE_MESSAGE);
            String muteBlockedMessage = stringValue(
                    moderation, "mute_blocked_message", DEFAULT_MUTE_BLOCKED_MESSAGE);
            String muteExpiredMessage = stringValue(
                    moderation, "mute_expired_message", DEFAULT_MUTE_EXPIRED_MESSAGE);
            boolean muteBlocksPrivateMessages = !(moderation.get("mute_blocks_private_messages")
                    instanceof Boolean value) || value;
            long rollingPeriodMs = parseRollingPeriod(
                    moderation.get("warning_rolling_period"));
            int alertThreshold = moderation.get("warning_alert_threshold") instanceof Number value
                    ? Math.max(1, value.intValue()) : 3;
            if (banMessage.isBlank()
                    || !banMessage.contains("{reason}")
                    || (!banMessage.contains("{time}") && !banMessage.contains("{expires_at}"))) {
                banMessage = DEFAULT_BAN_MESSAGE;
            }
            if (kickMessage.isBlank() || !kickMessage.contains("{reason}")) {
                kickMessage = DEFAULT_KICK_MESSAGE;
            }
            if (!warningMessage.contains("{reason}")) warningMessage = DEFAULT_WARNING_MESSAGE;
            if (!muteMessage.contains("{reason}") || !muteMessage.contains("{time}")) {
                muteMessage = DEFAULT_MUTE_MESSAGE;
            }
            if (!muteBlockedMessage.contains("{reason}")
                    || !muteBlockedMessage.contains("{time}")) {
                muteBlockedMessage = DEFAULT_MUTE_BLOCKED_MESSAGE;
            }
            return new ModerationConfig(
                    banMessage, kickMessage, warningMessage, muteMessage, unmuteMessage,
                    muteBlockedMessage, muteExpiredMessage, muteBlocksPrivateMessages,
                    rollingPeriodMs, alertThreshold);
        } catch (Exception e) {
            Essentials.LOGGER.warn(
                    "Failed to read moderation settings from essentials.yaml, using defaults: {}", e.toString());
            return defaults();
        }
    }

    private static ModerationConfig defaults() {
        return new ModerationConfig(
                DEFAULT_BAN_MESSAGE, DEFAULT_KICK_MESSAGE, DEFAULT_WARNING_MESSAGE,
                DEFAULT_MUTE_MESSAGE, DEFAULT_UNMUTE_MESSAGE, DEFAULT_MUTE_BLOCKED_MESSAGE,
                DEFAULT_MUTE_EXPIRED_MESSAGE, true, 30L * 86_400_000L, 3);
    }

    private static String stringValue(Map<?, ?> map, String key, String fallback) {
        return map.get(key) instanceof String value && !value.isBlank() ? value : fallback;
    }

    private static long parseRollingPeriod(Object value) {
        if (!(value instanceof String text)) {
            return 30L * 86_400_000L;
        }
        try {
            return DurationParser.parseMillis(text);
        } catch (IllegalArgumentException ignored) {
            return 30L * 86_400_000L;
        }
    }
}

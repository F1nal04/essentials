package f1nal.essentials.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import f1nal.essentials.Essentials;
import f1nal.essentials.update.ReleaseChannel;

/** Configuration for the once-per-server-session update check. */
public record UpdateConfig(
        boolean enabled,
        ReleaseChannel channel,
        boolean consoleNotifications,
        boolean playerNotifications,
        boolean opFallback,
        int requestTimeoutSeconds,
        int startupDelaySeconds,
        String notificationText,
        boolean clickableLink) {

    public static final int MIN_TIMEOUT_SECONDS = 2;
    public static final int MAX_TIMEOUT_SECONDS = 30;
    public static final int MIN_STARTUP_DELAY_SECONDS = 0;
    public static final int MAX_STARTUP_DELAY_SECONDS = 300;

    private static final UpdateConfig DEFAULTS = new UpdateConfig(
            true,
            ReleaseChannel.STABLE_ONLY,
            true,
            true,
            true,
            5,
            10,
            "Update available: {installed_version} -> {latest_version} ({channel}). {download_link}",
            true);

    public static UpdateConfig load() {
        Path configFile = ConfigPaths.configFile();
        if (!Files.exists(configFile)) {
            return DEFAULTS;
        }
        try {
            return parse(Files.readString(configFile, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Essentials.LOGGER.warn("Failed to read update settings from essentials.yaml, using defaults: {}",
                    e.toString());
            return DEFAULTS;
        }
    }

    static UpdateConfig parse(String yamlText) {
        try {
            Object root = new Yaml(new LoaderOptions()).load(yamlText);
            if (!(root instanceof Map<?, ?> rootMap)
                    || !(rootMap.get("updates") instanceof Map<?, ?> updates)) {
                return DEFAULTS;
            }

            return new UpdateConfig(
                    bool(updates, "enabled", DEFAULTS.enabled),
                    channel(updates.get("channel")),
                    bool(updates, "console_notifications", DEFAULTS.consoleNotifications),
                    bool(updates, "player_notifications", DEFAULTS.playerNotifications),
                    bool(updates, "op_fallback", DEFAULTS.opFallback),
                    boundedInt(updates, "request_timeout_seconds", DEFAULTS.requestTimeoutSeconds,
                            MIN_TIMEOUT_SECONDS, MAX_TIMEOUT_SECONDS),
                    boundedInt(updates, "startup_delay_seconds", DEFAULTS.startupDelaySeconds,
                            MIN_STARTUP_DELAY_SECONDS, MAX_STARTUP_DELAY_SECONDS),
                    string(updates, "notification_text", DEFAULTS.notificationText),
                    bool(updates, "clickable_link", DEFAULTS.clickableLink));
        } catch (Exception e) {
            return DEFAULTS;
        }
    }

    private static boolean bool(Map<?, ?> values, String key, boolean fallback) {
        return values.get(key) instanceof Boolean value ? value : fallback;
    }

    private static int boundedInt(Map<?, ?> values, String key, int fallback, int min, int max) {
        if (!(values.get(key) instanceof Number number)) {
            return fallback;
        }
        return Math.clamp(number.intValue(), min, max);
    }

    private static String string(Map<?, ?> values, String key, String fallback) {
        return values.get(key) instanceof String value && !value.isBlank() ? value : fallback;
    }

    private static ReleaseChannel channel(Object value) {
        if (!(value instanceof String text)) {
            return DEFAULTS.channel;
        }
        return switch (text.toLowerCase(Locale.ROOT).replace('-', '_')) {
            case "prerelease", "prereleases", "include_prereleases", "all" ->
                ReleaseChannel.INCLUDE_PRERELEASES;
            default -> ReleaseChannel.STABLE_ONLY;
        };
    }
}

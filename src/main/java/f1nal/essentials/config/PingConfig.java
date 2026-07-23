package f1nal.essentials.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import f1nal.essentials.Essentials;

/** Latency thresholds and feedback formats for /ping. */
public final class PingConfig {
    private static PingConfig instance;

    public enum NumberFormat {
        INTEGER,
        DECIMAL
    }

    public final int goodMaxMs;
    public final int moderateMaxMs;
    public final NumberFormat numberFormat;
    public final String selfFormat;
    public final String otherFormat;
    public final String unavailablePlayerFormat;
    public final String consoleRequiresPlayerFormat;
    public final String unavailableLatencyFormat;
    public final String insufficientAccessFormat;

    private PingConfig(int goodMaxMs, int moderateMaxMs, NumberFormat numberFormat,
            String selfFormat, String otherFormat, String unavailablePlayerFormat,
            String consoleRequiresPlayerFormat, String unavailableLatencyFormat,
            String insufficientAccessFormat) {
        this.goodMaxMs = goodMaxMs;
        this.moderateMaxMs = moderateMaxMs;
        this.numberFormat = numberFormat;
        this.selfFormat = selfFormat;
        this.otherFormat = otherFormat;
        this.unavailablePlayerFormat = unavailablePlayerFormat;
        this.consoleRequiresPlayerFormat = consoleRequiresPlayerFormat;
        this.unavailableLatencyFormat = unavailableLatencyFormat;
        this.insufficientAccessFormat = insufficientAccessFormat;
    }

    public static synchronized PingConfig get() {
        if (instance == null) instance = loadOrDefaults();
        return instance;
    }

    static PingConfig loadOrDefaults() {
        if (!Files.exists(ConfigPaths.configFile())) return defaults();
        try {
            return parse(Files.readString(ConfigPaths.configFile(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            Essentials.LOGGER.warn("Failed to read ping settings, using defaults: {}", e.toString());
            return defaults();
        }
    }

    static PingConfig parse(String yamlText) {
        PingConfig defaults = defaults();
        try {
            Object root = new Yaml(new LoaderOptions()).load(yamlText);
            if (!(root instanceof Map<?, ?> rootMap)
                    || !(rootMap.get("ping") instanceof Map<?, ?> map)) return defaults;

            int good = nonNegativeInt(map, "good_max_ms", defaults.goodMaxMs);
            int moderate = nonNegativeInt(map, "moderate_max_ms", defaults.moderateMaxMs);
            if (moderate < good) moderate = good;

            return new PingConfig(
                    good,
                    moderate,
                    numberFormat(map.get("number_format"), defaults.numberFormat),
                    text(map, "self_format", defaults.selfFormat),
                    text(map, "other_format", defaults.otherFormat),
                    text(map, "unavailable_player_format", defaults.unavailablePlayerFormat),
                    text(map, "console_requires_player_format", defaults.consoleRequiresPlayerFormat),
                    text(map, "unavailable_latency_format", defaults.unavailableLatencyFormat),
                    text(map, "insufficient_access_format", defaults.insufficientAccessFormat));
        } catch (Exception e) {
            return defaults;
        }
    }

    private static int nonNegativeInt(Map<?, ?> map, String key, int fallback) {
        if (!(map.get(key) instanceof Number value)) return fallback;
        int parsed = value.intValue();
        return parsed >= 0 ? parsed : fallback;
    }

    private static String text(Map<?, ?> map, String key, String fallback) {
        return map.get(key) instanceof String value && !value.isBlank() ? value : fallback;
    }

    private static NumberFormat numberFormat(Object value, NumberFormat fallback) {
        if (!(value instanceof String text)) return fallback;
        try {
            return NumberFormat.valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static PingConfig defaults() {
        return new PingConfig(
                100,
                200,
                NumberFormat.INTEGER,
                "&7Your ping is {color}{latency} ms&7.",
                "&d{player}&7's ping is {color}{latency} ms&7.",
                "&cThat player is offline or unavailable.",
                "&cThe server console must specify a player.",
                "&cLatency data is unavailable for {player}.",
                "&cYou do not have permission to view another player's ping.");
    }
}

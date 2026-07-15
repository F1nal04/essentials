package f1nal.essentials.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import f1nal.essentials.Essentials;

public final class ModerationConfig {

    private static final String DEFAULT_BAN_MESSAGE =
            "&cYou are banned from this server.\n&7Reason: &f{reason}\n&7Time remaining: &f{time}";
    private static final String DEFAULT_KICK_MESSAGE =
            "&cYou were kicked from this server.\n&7Reason: &f{reason}";

    private static ModerationConfig instance;

    public final String banMessage;
    public final String kickMessage;

    private ModerationConfig(String banMessage, String kickMessage) {
        this.banMessage = banMessage;
        this.kickMessage = kickMessage;
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
            if (banMessage.isBlank()
                    || !banMessage.contains("{reason}")
                    || (!banMessage.contains("{time}") && !banMessage.contains("{expires_at}"))) {
                banMessage = DEFAULT_BAN_MESSAGE;
            }
            if (kickMessage.isBlank() || !kickMessage.contains("{reason}")) {
                kickMessage = DEFAULT_KICK_MESSAGE;
            }
            return new ModerationConfig(banMessage, kickMessage);
        } catch (Exception e) {
            Essentials.LOGGER.warn(
                    "Failed to read moderation settings from essentials.yaml, using defaults: {}", e.toString());
            return defaults();
        }
    }

    private static ModerationConfig defaults() {
        return new ModerationConfig(DEFAULT_BAN_MESSAGE, DEFAULT_KICK_MESSAGE);
    }
}

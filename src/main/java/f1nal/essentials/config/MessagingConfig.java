package f1nal.essentials.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import f1nal.essentials.Essentials;

/** Formatting and policy settings shared by the private-messaging commands. */
public final class MessagingConfig {

    private static MessagingConfig instance;

    public final String incomingFormat;
    public final String outgoingFormat;
    public final String replyFormat;
    public final String spyFormat;
    public final String messageAllFormat;
    public final String ignoredPlayerFormat;
    public final String unavailableTargetFormat;
    public final String missingReplyTargetFormat;
    public final boolean staffBypassesIgnore;
    public final boolean consoleBypassesIgnore;

    private MessagingConfig(String incomingFormat, String outgoingFormat, String replyFormat,
            String spyFormat, String messageAllFormat, String ignoredPlayerFormat,
            String unavailableTargetFormat, String missingReplyTargetFormat,
            boolean staffBypassesIgnore, boolean consoleBypassesIgnore) {
        this.incomingFormat = incomingFormat;
        this.outgoingFormat = outgoingFormat;
        this.replyFormat = replyFormat;
        this.spyFormat = spyFormat;
        this.messageAllFormat = messageAllFormat;
        this.ignoredPlayerFormat = ignoredPlayerFormat;
        this.unavailableTargetFormat = unavailableTargetFormat;
        this.missingReplyTargetFormat = missingReplyTargetFormat;
        this.staffBypassesIgnore = staffBypassesIgnore;
        this.consoleBypassesIgnore = consoleBypassesIgnore;
    }

    public static synchronized MessagingConfig get() {
        if (instance == null) {
            instance = loadOrDefaults();
        }
        return instance;
    }

    static MessagingConfig loadOrDefaults() {
        Path path = ConfigPaths.configFile();
        if (!Files.exists(path)) return defaults();
        try {
            return parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Essentials.LOGGER.warn("Failed to read messaging settings, using defaults: {}", e.toString());
            return defaults();
        }
    }

    static MessagingConfig parse(String yamlText) {
        MessagingConfig defaults = defaults();
        try {
            Object root = new Yaml(new LoaderOptions()).load(yamlText);
            if (!(root instanceof Map<?, ?> rootMap)
                    || !(rootMap.get("messaging") instanceof Map<?, ?> map)) {
                return defaults;
            }
            return new MessagingConfig(
                    text(map, "incoming_format", defaults.incomingFormat),
                    text(map, "outgoing_format", defaults.outgoingFormat),
                    text(map, "reply_format", defaults.replyFormat),
                    text(map, "spy_format", defaults.spyFormat),
                    text(map, "message_all_format", defaults.messageAllFormat),
                    text(map, "ignored_player_format", defaults.ignoredPlayerFormat),
                    text(map, "unavailable_target_format", defaults.unavailableTargetFormat),
                    text(map, "missing_reply_target_format", defaults.missingReplyTargetFormat),
                    bool(map, "staff_bypasses_ignore", defaults.staffBypassesIgnore),
                    bool(map, "console_bypasses_ignore", defaults.consoleBypassesIgnore));
        } catch (Exception e) {
            return defaults;
        }
    }

    private static String text(Map<?, ?> map, String key, String fallback) {
        return map.get(key) instanceof String value && !value.isBlank() ? value : fallback;
    }

    private static boolean bool(Map<?, ?> map, String key, boolean fallback) {
        return map.get(key) instanceof Boolean value ? value : fallback;
    }

    private static MessagingConfig defaults() {
        return new MessagingConfig(
                "&8[&d{sender} &5-> &fYou&8] &7{message}",
                "&8[&fYou &5-> &d{recipient}&8] &7{message}",
                "&8[&fYou &5-> &d{recipient}&8] &7{message}",
                "&8[&7Spy&8] &7{sender} &8-> &7{recipient}&8: &7{message}",
                "&8[&5Announcement&8] &d{message}",
                "&c{recipient} is not accepting your private messages.",
                "&cThat player is offline or unavailable.",
                "&cYou do not have anyone to reply to.",
                true, true);
    }
}

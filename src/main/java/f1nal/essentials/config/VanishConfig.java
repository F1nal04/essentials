package f1nal.essentials.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import f1nal.essentials.Essentials;

/** Persistence, concealment, and feedback settings for vanish mode. */
public final class VanishConfig {
    private static VanishConfig instance;

    public enum ChatBehavior { BLOCK, STAFF_ONLY }

    public final boolean persistState;
    public final boolean hideFromTabList;
    public final boolean preventMobTargeting;
    public final boolean preventCollision;
    public final boolean suppressAnnouncements;
    public final ChatBehavior chatBehavior;
    public final String enabledMessage;
    public final String disabledMessage;
    public final String joinMessage;
    public final String leaveMessage;
    public final String chatBlockedMessage;
    public final String staffChatFormat;

    private VanishConfig(boolean persistState, boolean hideFromTabList,
            boolean preventMobTargeting, boolean preventCollision,
            boolean suppressAnnouncements, ChatBehavior chatBehavior,
            String enabledMessage, String disabledMessage, String joinMessage,
            String leaveMessage, String chatBlockedMessage, String staffChatFormat) {
        this.persistState = persistState;
        this.hideFromTabList = hideFromTabList;
        this.preventMobTargeting = preventMobTargeting;
        this.preventCollision = preventCollision;
        this.suppressAnnouncements = suppressAnnouncements;
        this.chatBehavior = chatBehavior;
        this.enabledMessage = enabledMessage;
        this.disabledMessage = disabledMessage;
        this.joinMessage = joinMessage;
        this.leaveMessage = leaveMessage;
        this.chatBlockedMessage = chatBlockedMessage;
        this.staffChatFormat = staffChatFormat;
    }

    public static synchronized VanishConfig get() {
        if (instance == null) instance = loadOrDefaults();
        return instance;
    }

    static VanishConfig loadOrDefaults() {
        if (!Files.exists(ConfigPaths.configFile())) return defaults();
        try {
            return parse(Files.readString(ConfigPaths.configFile(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            Essentials.LOGGER.warn("Failed to read vanish settings, using defaults: {}", e.toString());
            return defaults();
        }
    }

    static VanishConfig parse(String yamlText) {
        VanishConfig defaults = defaults();
        try {
            Object root = new Yaml(new LoaderOptions()).load(yamlText);
            if (!(root instanceof Map<?, ?> rootMap)
                    || !(rootMap.get("vanish") instanceof Map<?, ?> map)) return defaults;
            return new VanishConfig(
                    bool(map, "persist_state", defaults.persistState),
                    bool(map, "hide_from_tab_list", defaults.hideFromTabList),
                    bool(map, "prevent_mob_targeting", defaults.preventMobTargeting),
                    bool(map, "prevent_collision", defaults.preventCollision),
                    bool(map, "suppress_announcements", defaults.suppressAnnouncements),
                    chatBehavior(map.get("chat_behavior"), defaults.chatBehavior),
                    text(map, "enabled_message", defaults.enabledMessage),
                    text(map, "disabled_message", defaults.disabledMessage),
                    text(map, "join_message", defaults.joinMessage),
                    text(map, "leave_message", defaults.leaveMessage),
                    text(map, "chat_blocked_message", defaults.chatBlockedMessage),
                    text(map, "staff_chat_format", defaults.staffChatFormat));
        } catch (Exception e) {
            return defaults;
        }
    }

    private static boolean bool(Map<?, ?> map, String key, boolean fallback) {
        return map.get(key) instanceof Boolean value ? value : fallback;
    }

    private static String text(Map<?, ?> map, String key, String fallback) {
        return map.get(key) instanceof String value && !value.isBlank() ? value : fallback;
    }

    private static ChatBehavior chatBehavior(Object value, ChatBehavior fallback) {
        if (!(value instanceof String text)) return fallback;
        try {
            return ChatBehavior.valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static VanishConfig defaults() {
        return new VanishConfig(true, true, true, true, true, ChatBehavior.BLOCK,
                "&aVanish enabled for {player}.",
                "&eVanish disabled for {player}.",
                "&7{player} joined while vanished.",
                "&7{player} left while vanished.",
                "&cYou cannot use public chat while vanished.",
                "&8[&7Vanish&8] &d{player}&8: &7{message}");
    }
}

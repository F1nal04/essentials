package f1nal.essentials.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import f1nal.essentials.Essentials;
import f1nal.essentials.home.HomeNames;

/** Limits, timing, name rules, and feedback for player homes. */
public final class HomeConfig {
    private static final int DEFAULT_LIMIT = 1;
    private static final int DEFAULT_MAXIMUM = 5;
    private static final int MAX_HOMES = 256;
    private static final int DEFAULT_WARMUP_SECONDS = 3;
    private static final int DEFAULT_COOLDOWN_SECONDS = 30;
    private static final int MAX_DELAY_SECONDS = 3600;

    private static HomeConfig instance;

    public final int defaultLimit;
    public final int maximumLimit;
    public final boolean allowCrossDimension;
    public final int warmupSeconds;
    public final int cooldownSeconds;
    public final boolean cancelOnMovement;
    public final boolean cancelOnDamage;
    public final String defaultName;
    public final Pattern namePattern;
    public final String setMessage;
    public final String duplicateMessage;
    public final String invalidNameMessage;
    public final String limitMessage;
    public final String deletedMessage;
    public final String missingMessage;
    public final String noHomesMessage;
    public final String listMessage;
    public final String teleportedMessage;
    public final String warmupMessage;
    public final String cooldownMessage;
    public final String movementCancelledMessage;
    public final String damageCancelledMessage;
    public final String unavailableWorldMessage;
    public final String unsafeDestinationMessage;
    public final String crossDimensionMessage;
    public final String playerOnlyMessage;
    public final String alreadyPendingMessage;
    public final String saveFailedMessage;

    private HomeConfig(int defaultLimit, int maximumLimit, boolean allowCrossDimension,
            int warmupSeconds, int cooldownSeconds, boolean cancelOnMovement,
            boolean cancelOnDamage, String defaultName, Pattern namePattern,
            String setMessage, String duplicateMessage, String invalidNameMessage,
            String limitMessage, String deletedMessage, String missingMessage,
            String noHomesMessage, String listMessage, String teleportedMessage,
            String warmupMessage, String cooldownMessage, String movementCancelledMessage,
            String damageCancelledMessage, String unavailableWorldMessage,
            String unsafeDestinationMessage, String crossDimensionMessage,
            String playerOnlyMessage, String alreadyPendingMessage, String saveFailedMessage) {
        this.defaultLimit = defaultLimit;
        this.maximumLimit = maximumLimit;
        this.allowCrossDimension = allowCrossDimension;
        this.warmupSeconds = warmupSeconds;
        this.cooldownSeconds = cooldownSeconds;
        this.cancelOnMovement = cancelOnMovement;
        this.cancelOnDamage = cancelOnDamage;
        this.defaultName = defaultName;
        this.namePattern = namePattern;
        this.setMessage = setMessage;
        this.duplicateMessage = duplicateMessage;
        this.invalidNameMessage = invalidNameMessage;
        this.limitMessage = limitMessage;
        this.deletedMessage = deletedMessage;
        this.missingMessage = missingMessage;
        this.noHomesMessage = noHomesMessage;
        this.listMessage = listMessage;
        this.teleportedMessage = teleportedMessage;
        this.warmupMessage = warmupMessage;
        this.cooldownMessage = cooldownMessage;
        this.movementCancelledMessage = movementCancelledMessage;
        this.damageCancelledMessage = damageCancelledMessage;
        this.unavailableWorldMessage = unavailableWorldMessage;
        this.unsafeDestinationMessage = unsafeDestinationMessage;
        this.crossDimensionMessage = crossDimensionMessage;
        this.playerOnlyMessage = playerOnlyMessage;
        this.alreadyPendingMessage = alreadyPendingMessage;
        this.saveFailedMessage = saveFailedMessage;
    }

    public static synchronized HomeConfig get() {
        if (instance == null) instance = loadOrDefaults();
        return instance;
    }

    static HomeConfig loadOrDefaults() {
        if (!Files.exists(ConfigPaths.configFile())) return defaults();
        try {
            return parse(Files.readString(ConfigPaths.configFile(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            Essentials.LOGGER.warn("Failed to read home settings, using defaults: {}", e.toString());
            return defaults();
        }
    }

    static HomeConfig parse(String yamlText) {
        HomeConfig fallback = defaults();
        try {
            Object root = new Yaml(new LoaderOptions()).load(yamlText);
            if (!(root instanceof Map<?, ?> rootMap)
                    || !(rootMap.get("homes") instanceof Map<?, ?> map)) {
                return fallback;
            }
            int maximum = limit(map, "maximum_limit", fallback.maximumLimit, 1, MAX_HOMES);
            int defaultFallback = Math.min(fallback.defaultLimit, maximum);
            Pattern pattern = HomeNames.compileOrDefault(string(map.get("name_pattern")));
            String defaultName = resolveDefaultName(map, pattern, fallback.defaultName);
            if (HomeNames.validate(defaultName, pattern) != HomeNames.Validity.OK) {
                pattern = HomeNames.compileOrDefault(null);
                defaultName = fallback.defaultName;
            }
            return new HomeConfig(
                    limit(map, "default_limit", defaultFallback, 1, maximum),
                    maximum,
                    bool(map, "allow_cross_dimension", fallback.allowCrossDimension),
                    seconds(map, "warmup_seconds", fallback.warmupSeconds),
                    seconds(map, "cooldown_seconds", fallback.cooldownSeconds),
                    bool(map, "cancel_on_movement", fallback.cancelOnMovement),
                    bool(map, "cancel_on_damage", fallback.cancelOnDamage),
                    defaultName,
                    pattern,
                    text(map, "set_message", fallback.setMessage),
                    text(map, "duplicate_message", fallback.duplicateMessage),
                    text(map, "invalid_name_message", fallback.invalidNameMessage),
                    text(map, "limit_message", fallback.limitMessage),
                    text(map, "deleted_message", fallback.deletedMessage),
                    text(map, "missing_message", fallback.missingMessage),
                    text(map, "no_homes_message", fallback.noHomesMessage),
                    text(map, "list_message", fallback.listMessage),
                    text(map, "teleported_message", fallback.teleportedMessage),
                    text(map, "warmup_message", fallback.warmupMessage),
                    text(map, "cooldown_message", fallback.cooldownMessage),
                    text(map, "movement_cancelled_message", fallback.movementCancelledMessage),
                    text(map, "damage_cancelled_message", fallback.damageCancelledMessage),
                    text(map, "unavailable_world_message", fallback.unavailableWorldMessage),
                    text(map, "unsafe_destination_message", fallback.unsafeDestinationMessage),
                    text(map, "cross_dimension_message", fallback.crossDimensionMessage),
                    text(map, "player_only_message", fallback.playerOnlyMessage),
                    text(map, "already_pending_message", fallback.alreadyPendingMessage),
                    text(map, "save_failed_message", fallback.saveFailedMessage));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String resolveDefaultName(Map<?, ?> map, Pattern pattern, String fallback) {
        Object value = map.get("default_name");
        if (!(value instanceof String text) || text.isBlank()) return fallback;
        String trimmed = text.trim();
        if (HomeNames.validate(trimmed, pattern) != HomeNames.Validity.OK) {
            if (HomeNames.validate(HomeNames.FALLBACK_NAME, pattern) == HomeNames.Validity.OK) {
                return HomeNames.FALLBACK_NAME;
            }
            return fallback;
        }
        return trimmed;
    }

    private static boolean bool(Map<?, ?> map, String key, boolean fallback) {
        return map.get(key) instanceof Boolean value ? value : fallback;
    }

    private static int seconds(Map<?, ?> map, String key, int fallback) {
        return limit(map, key, fallback, 0, MAX_DELAY_SECONDS);
    }

    private static int limit(Map<?, ?> map, String key, int fallback, int min, int max) {
        Object value = map.get(key);
        int parsed;
        if (value instanceof Number number) {
            parsed = number.intValue();
        } else if (value instanceof String text) {
            try {
                parsed = Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        } else {
            return fallback;
        }
        return parsed >= min && parsed <= max ? parsed : fallback;
    }

    private static String text(Map<?, ?> map, String key, String fallback) {
        return map.get(key) instanceof String value && !value.isBlank() ? value : fallback;
    }

    private static String string(Object value) {
        return value instanceof String text ? text : null;
    }

    private static HomeConfig defaults() {
        return new HomeConfig(DEFAULT_LIMIT, DEFAULT_MAXIMUM, true,
                DEFAULT_WARMUP_SECONDS, DEFAULT_COOLDOWN_SECONDS, true, true,
                HomeNames.FALLBACK_NAME, HomeNames.compileOrDefault(null),
                "&aHome &d{name}&a set.",
                "&cYou already have a home named &d{name}&c.",
                "&cThat home name is not allowed.",
                "&cYou have reached your home limit of {limit}.",
                "&aHome &d{name}&a deleted.",
                "&cYou do not have a home named &d{name}&c.",
                "&7You have no homes.",
                "&7Homes ({count}): ",
                "&aTeleported to &d{name}&a.",
                "&7Teleporting to &d{name}&7 in &d{seconds}&7 seconds. Do not move or take damage.",
                "&cYou must wait {seconds} seconds before using /home again.",
                "&cHome teleport cancelled because you moved.",
                "&cHome teleport cancelled because you took damage.",
                "&cThe world for home &d{name}&c is unavailable.",
                "&cNo safe arrival position could be found near &d{name}&c.",
                "&cCross-dimension home teleports are disabled.",
                "&cYou must be a player to use this command.",
                "&cYou already have a pending home teleport.",
                "&cThe home could not be saved.");
    }
}

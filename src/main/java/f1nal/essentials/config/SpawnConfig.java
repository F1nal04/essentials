package f1nal.essentials.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import f1nal.essentials.Essentials;

/** Routing, timing, cancellation, and feedback settings for server spawn. */
public final class SpawnConfig {
    private static final int DEFAULT_WARMUP_SECONDS = 3;
    private static final int DEFAULT_COOLDOWN_SECONDS = 30;
    private static final int MAX_DELAY_SECONDS = 3600;

    private static SpawnConfig instance;

    public final boolean firstJoin;
    public final boolean respawn;
    public final int warmupSeconds;
    public final int cooldownSeconds;
    public final boolean cancelOnMovement;
    public final boolean cancelOnDamage;
    public final String setMessage;
    public final String teleportedMessage;
    public final String warmupMessage;
    public final String cooldownMessage;
    public final String movementCancelledMessage;
    public final String damageCancelledMessage;
    public final String noSpawnMessage;
    public final String unavailableWorldMessage;
    public final String unsafeDestinationMessage;
    public final String playerOnlyMessage;
    public final String alreadyPendingMessage;
    public final String saveFailedMessage;

    private SpawnConfig(boolean firstJoin, boolean respawn, int warmupSeconds,
            int cooldownSeconds, boolean cancelOnMovement, boolean cancelOnDamage,
            String setMessage, String teleportedMessage, String warmupMessage,
            String cooldownMessage, String movementCancelledMessage,
            String damageCancelledMessage, String noSpawnMessage,
            String unavailableWorldMessage, String unsafeDestinationMessage,
            String playerOnlyMessage, String alreadyPendingMessage,
            String saveFailedMessage) {
        this.firstJoin = firstJoin;
        this.respawn = respawn;
        this.warmupSeconds = warmupSeconds;
        this.cooldownSeconds = cooldownSeconds;
        this.cancelOnMovement = cancelOnMovement;
        this.cancelOnDamage = cancelOnDamage;
        this.setMessage = setMessage;
        this.teleportedMessage = teleportedMessage;
        this.warmupMessage = warmupMessage;
        this.cooldownMessage = cooldownMessage;
        this.movementCancelledMessage = movementCancelledMessage;
        this.damageCancelledMessage = damageCancelledMessage;
        this.noSpawnMessage = noSpawnMessage;
        this.unavailableWorldMessage = unavailableWorldMessage;
        this.unsafeDestinationMessage = unsafeDestinationMessage;
        this.playerOnlyMessage = playerOnlyMessage;
        this.alreadyPendingMessage = alreadyPendingMessage;
        this.saveFailedMessage = saveFailedMessage;
    }

    public static synchronized SpawnConfig get() {
        if (instance == null) instance = loadOrDefaults();
        return instance;
    }

    static SpawnConfig loadOrDefaults() {
        if (!Files.exists(ConfigPaths.configFile())) return defaults();
        try {
            return parse(Files.readString(ConfigPaths.configFile(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            Essentials.LOGGER.warn("Failed to read spawn settings, using defaults: {}", e.toString());
            return defaults();
        }
    }

    static SpawnConfig parse(String yamlText) {
        SpawnConfig fallback = defaults();
        try {
            Object root = new Yaml(new LoaderOptions()).load(yamlText);
            if (!(root instanceof Map<?, ?> rootMap)
                    || !(rootMap.get("spawn") instanceof Map<?, ?> map)) {
                return fallback;
            }
            return new SpawnConfig(
                    bool(map, "first_join", fallback.firstJoin),
                    bool(map, "respawn", fallback.respawn),
                    seconds(map, "warmup_seconds", fallback.warmupSeconds),
                    seconds(map, "cooldown_seconds", fallback.cooldownSeconds),
                    bool(map, "cancel_on_movement", fallback.cancelOnMovement),
                    bool(map, "cancel_on_damage", fallback.cancelOnDamage),
                    text(map, "set_message", fallback.setMessage),
                    text(map, "teleported_message", fallback.teleportedMessage),
                    text(map, "warmup_message", fallback.warmupMessage),
                    text(map, "cooldown_message", fallback.cooldownMessage),
                    text(map, "movement_cancelled_message", fallback.movementCancelledMessage),
                    text(map, "damage_cancelled_message", fallback.damageCancelledMessage),
                    text(map, "no_spawn_message", fallback.noSpawnMessage),
                    text(map, "unavailable_world_message", fallback.unavailableWorldMessage),
                    text(map, "unsafe_destination_message", fallback.unsafeDestinationMessage),
                    text(map, "player_only_message", fallback.playerOnlyMessage),
                    text(map, "already_pending_message", fallback.alreadyPendingMessage),
                    text(map, "save_failed_message", fallback.saveFailedMessage));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static boolean bool(Map<?, ?> map, String key, boolean fallback) {
        return map.get(key) instanceof Boolean value ? value : fallback;
    }

    private static int seconds(Map<?, ?> map, String key, int fallback) {
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
        return parsed >= 0 && parsed <= MAX_DELAY_SECONDS ? parsed : fallback;
    }

    private static String text(Map<?, ?> map, String key, String fallback) {
        return map.get(key) instanceof String value && !value.isBlank() ? value : fallback;
    }

    private static SpawnConfig defaults() {
        return new SpawnConfig(false, false, DEFAULT_WARMUP_SECONDS,
                DEFAULT_COOLDOWN_SECONDS, true, true,
                "&aEssentials spawn set.",
                "&aTeleported to spawn.",
                "&7Teleporting to spawn in &d{seconds}&7 seconds. Do not move or take damage.",
                "&cYou must wait {seconds} seconds before using /spawn again.",
                "&cSpawn teleport cancelled because you moved.",
                "&cSpawn teleport cancelled because you took damage.",
                "&cNo Essentials spawn has been configured.",
                "&cThe configured spawn world is unavailable.",
                "&cNo safe arrival position could be found near spawn.",
                "&cYou must be a player to use this command.",
                "&cYou already have a pending spawn teleport.",
                "&cThe spawn location could not be saved.");
    }
}

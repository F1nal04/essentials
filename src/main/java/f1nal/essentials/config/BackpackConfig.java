package f1nal.essentials.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import net.fabricmc.loader.api.FabricLoader;

public final class BackpackConfig {

    public enum Mode {
        PER_PLAYER,
        SERVERWIDE,
        ENDER_CHEST;

        static Mode fromString(String value) {
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "serverwide" -> SERVERWIDE;
                case "ender_chest" -> ENDER_CHEST;
                default -> PER_PLAYER;
            };
        }
    }

    private static BackpackConfig INSTANCE = null;

    public final Mode mode;

    private BackpackConfig(Mode mode) {
        this.mode = mode;
    }

    public static BackpackConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    private static BackpackConfig load() {
        Path cfg = FabricLoader.getInstance().getConfigDir().resolve("essentials.yaml");
        if (!Files.exists(cfg)) {
            return defaults();
        }
        try {
            return parse(Files.readString(cfg, StandardCharsets.UTF_8));
        } catch (Exception e) {
            f1nal.essentials.Essentials.LOGGER.warn("Failed to read backpack settings from essentials.yaml, using defaults: {}", e.toString());
            return defaults();
        }
    }

    static BackpackConfig parse(String yamlText) {
        try {
            Object root = new Yaml(new LoaderOptions()).load(yamlText);
            if (!(root instanceof Map<?, ?> map)) {
                return defaults();
            }
            Object backpackObj = map.get("backpack");
            if (!(backpackObj instanceof Map<?, ?> backpack)) {
                return defaults();
            }

            String mode = backpack.get("mode") instanceof String ? (String) backpack.get("mode") : "per_player";

            return new BackpackConfig(Mode.fromString(mode));
        } catch (Exception e) {
            f1nal.essentials.Essentials.LOGGER.warn("Failed to read backpack settings from essentials.yaml, using defaults: {}", e.toString());
            return defaults();
        }
    }

    private static BackpackConfig defaults() {
        return new BackpackConfig(Mode.PER_PLAYER); // Default to per-player mode
    }
}

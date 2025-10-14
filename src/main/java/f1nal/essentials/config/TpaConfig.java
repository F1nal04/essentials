package f1nal.essentials.config;

import net.fabricmc.loader.api.FabricLoader;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Configuration for TPA feature (timeout and cancel cooldown).
 */
public final class TpaConfig {

    private static final int DEFAULT_TIMEOUT_SECONDS = 60; // 60s timeout
    private static final int DEFAULT_COOLDOWN_SECONDS = 10; // 10s cooldown

    public final int timeoutSeconds;
    public final int cooldownSeconds;

    private static TpaConfig INSTANCE;

    private TpaConfig(int timeoutSeconds, int cooldownSeconds) {
        this.timeoutSeconds = timeoutSeconds;
        this.cooldownSeconds = cooldownSeconds;
    }

    public static synchronized TpaConfig get() {
        if (INSTANCE == null) {
            INSTANCE = loadOrDefaults();
        }
        return INSTANCE;
    }

    static TpaConfig loadOrDefaults() {
        Path cfg = FabricLoader.getInstance().getConfigDir().resolve("essentials.yaml");
        if (!Files.exists(cfg)) {
            return defaults();
        }
        try (Reader reader = Files.newBufferedReader(cfg, StandardCharsets.UTF_8)) {
            Yaml yaml = new Yaml(new LoaderOptions());
            Object root = yaml.load(reader);
            if (!(root instanceof Map<?, ?> map)) {
                return defaults();
            }
            Object tpaObj = map.get("tpa");
            if (!(tpaObj instanceof Map<?, ?> tpa)) {
                return defaults();
            }
            int timeout = coerceInt(tpa.get("timeout_seconds"), DEFAULT_TIMEOUT_SECONDS);
            int cooldown = coerceInt(tpa.get("cooldown_seconds"), DEFAULT_COOLDOWN_SECONDS);
            // Prevent nonsensical values
            if (timeout < 1) timeout = DEFAULT_TIMEOUT_SECONDS;
            if (cooldown < 0) cooldown = DEFAULT_COOLDOWN_SECONDS;
            return new TpaConfig(timeout, cooldown);
        } catch (IOException e) {
            return defaults();
        }
    }

    private static TpaConfig defaults() {
        return new TpaConfig(DEFAULT_TIMEOUT_SECONDS, DEFAULT_COOLDOWN_SECONDS);
    }

    private static int coerceInt(Object value, int def) {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }
}

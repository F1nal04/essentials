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

public final class BackConfig {
    private static final int DEFAULT_WINDOW_SECONDS = 120;

    public final int windowSeconds;

    private static BackConfig INSTANCE;

    private BackConfig(int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public static synchronized BackConfig get() {
        if (INSTANCE == null) {
            INSTANCE = loadOrDefaults();
        }
        return INSTANCE;
    }

    static BackConfig loadOrDefaults() {
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
            Object backObj = map.get("back");
            if (!(backObj instanceof Map<?, ?> back)) {
                return defaults();
            }
            int window = coerceInt(back.get("window_seconds"), DEFAULT_WINDOW_SECONDS);
            if (window < 1) window = DEFAULT_WINDOW_SECONDS;
            return new BackConfig(window);
        } catch (IOException e) {
            return defaults();
        }
    }

    private static BackConfig defaults() {
        return new BackConfig(DEFAULT_WINDOW_SECONDS);
    }

    private static int coerceInt(Object value, int def) {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {}
        }
        return def;
    }
}

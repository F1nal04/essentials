package f1nal.essentials.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import net.fabricmc.loader.api.FabricLoader;

public final class BackpackConfig {

    private static BackpackConfig INSTANCE = null;

    public final boolean perPlayer;

    private BackpackConfig(boolean perPlayer) {
        this.perPlayer = perPlayer;
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
        try (Reader reader = Files.newBufferedReader(cfg, StandardCharsets.UTF_8)) {
            Yaml yaml = new Yaml(new LoaderOptions());
            Object root = yaml.load(reader);
            if (!(root instanceof Map<?, ?> map)) {
                return defaults();
            }
            Object backpackObj = map.get("backpack");
            if (!(backpackObj instanceof Map<?, ?> backpack)) {
                return defaults();
            }

            String mode = backpack.get("mode") instanceof String ? (String) backpack.get("mode") : "per_player";
            boolean perPlayer = mode.equalsIgnoreCase("per_player");

            return new BackpackConfig(perPlayer);
        } catch (IOException e) {
            return defaults();
        }
    }

    private static BackpackConfig defaults() {
        return new BackpackConfig(true); // Default to per-player mode
    }
}

package f1nal.essentials.config;

import java.nio.file.Path;

import net.fabricmc.loader.api.FabricLoader;

/** Central location for Essentials configuration and server-scoped data. */
public final class ConfigPaths {

    private ConfigPaths() {
    }

    public static Path directory() {
        return FabricLoader.getInstance().getConfigDir().resolve("essentials");
    }

    public static Path configFile() {
        return directory().resolve("essentials.yaml");
    }

    public static Path databaseFile() {
        return directory().resolve("essentials.db");
    }

    public static Path ignoredPlayersFile() {
        return directory().resolve("ignored-players.properties");
    }

    static Path legacyConfigFile(Path configDir) {
        return configDir.resolve("essentials.yaml");
    }

    static Path configFile(Path configDir) {
        return configDir.resolve("essentials").resolve("essentials.yaml");
    }
}

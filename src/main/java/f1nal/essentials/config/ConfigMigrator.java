package f1nal.essentials.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import f1nal.essentials.Essentials;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Creates the config on first run and migrates it when a mod update changes
 * the config schema: user settings are kept, new keys get their defaults, and
 * the previous file is backed up. All merge decisions live in
 * {@link ConfigMerger}; this class only does file I/O and logging.
 */
public final class ConfigMigrator {

    private ConfigMigrator() {
    }

    public static void run() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path target = ConfigPaths.configFile(configDir);

        String template = readBundledDefault();
        if (template == null) {
            Essentials.LOGGER.warn("Missing bundled essentials.default.yaml resource; skipping config check.");
            return;
        }

        try {
            Files.createDirectories(target.getParent());
            moveLegacyConfigIfNeeded(configDir, target);
            if (!Files.exists(target)) {
                Files.writeString(target, template, StandardCharsets.UTF_8);
                Essentials.LOGGER.info("Wrote default config to {}", target.toAbsolutePath());
                return;
            }

            String userText = Files.readString(target, StandardCharsets.UTF_8);
            ConfigMerger.Result result = ConfigMerger.merge(template, userText);
            switch (result.status()) {
                case NO_CHANGE -> {
                }
                case UNREADABLE_USER -> Essentials.LOGGER.warn(
                        "Could not read {} ({}); running on default settings. Delete the file to regenerate a fresh default config.",
                        target.toAbsolutePath(), result.reason());
                case UNSUPPORTED_TEMPLATE -> Essentials.LOGGER.warn(
                        "Essentials config schema changed with this mod update, but was not migrated automatically ({}):\n{}Please update {} by hand.",
                        result.reason(), changeList(result), target.toAbsolutePath());
                case MERGED -> {
                    Path backup = target.resolveSibling("essentials.yaml.bak");
                    Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
                    Files.writeString(target, result.mergedText(), StandardCharsets.UTF_8);
                    Essentials.LOGGER.warn(
                            "Essentials config schema changed with this mod update. Your config was migrated:\n{}"
                                    + "Previous config backed up to {}. Your existing settings were preserved.",
                            changeList(result), backup.toAbsolutePath());
                }
            }
        } catch (IOException e) {
            Essentials.LOGGER.warn("Failed to check/migrate config: {}", e.toString());
        }
    }

    static void moveLegacyConfigIfNeeded(Path configDir, Path target) throws IOException {
        Path legacy = ConfigPaths.legacyConfigFile(configDir);
        if (!Files.exists(legacy)) {
            return;
        }
        if (Files.exists(target)) {
            Essentials.LOGGER.warn(
                    "Both legacy config {} and current config {} exist; using the current config and leaving the legacy file untouched.",
                    legacy.toAbsolutePath(), target.toAbsolutePath());
            return;
        }
        Files.createDirectories(target.getParent());
        Files.move(legacy, target);
        Essentials.LOGGER.info("Moved Essentials config from {} to {}", legacy.toAbsolutePath(), target.toAbsolutePath());
    }

    private static String changeList(ConfigMerger.Result result) {
        StringBuilder sb = new StringBuilder();
        for (ConfigMerger.Entry e : result.added()) {
            sb.append("  + ").append(e.path()).append(" (added, default: ")
                    .append(displayValue(e.value())).append(")\n");
        }
        for (ConfigMerger.Entry e : result.removed()) {
            sb.append("  - ").append(e.path()).append(" (removed; old value: ")
                    .append(displayValue(e.value())).append(")\n");
        }
        for (ConfigMerger.Entry e : result.reset()) {
            sb.append("  ~ ").append(e.path()).append(" (reset to default; old value: ")
                    .append(displayValue(e.value())).append(")\n");
        }
        return sb.toString();
    }

    private static String displayValue(Object value) {
        return String.valueOf(value).replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String readBundledDefault() {
        try (InputStream in = ConfigMigrator.class.getClassLoader().getResourceAsStream("essentials.default.yaml")) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}

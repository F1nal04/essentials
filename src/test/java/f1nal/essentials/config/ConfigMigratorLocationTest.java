package f1nal.essentials.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigMigratorLocationTest {

    @TempDir
    Path tempDir;

    @Test
    void movesLegacyConfigIntoEssentialsDirectory() throws Exception {
        Path legacy = ConfigPaths.legacyConfigFile(tempDir);
        Path target = ConfigPaths.configFile(tempDir);
        Files.writeString(legacy, "tag: {}\n");

        ConfigMigrator.moveLegacyConfigIfNeeded(tempDir, target);

        assertFalse(Files.exists(legacy));
        assertTrue(Files.exists(target));
        assertEquals("tag: {}\n", Files.readString(target));
    }

    @Test
    void currentConfigWinsWhenBothLocationsExist() throws Exception {
        Path legacy = ConfigPaths.legacyConfigFile(tempDir);
        Path target = ConfigPaths.configFile(tempDir);
        Files.createDirectories(target.getParent());
        Files.writeString(legacy, "legacy");
        Files.writeString(target, "current");

        ConfigMigrator.moveLegacyConfigIfNeeded(tempDir, target);

        assertEquals("legacy", Files.readString(legacy));
        assertEquals("current", Files.readString(target));
    }
}

package f1nal.essentials.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpawnStoreTest {
    @TempDir Path directory;

    @Test
    void roundTripsDimensionPositionAndRotation() throws Exception {
        SpawnLocation expected = new SpawnLocation(
                "minecraft:the_nether", 12.25, 70.5, -31.75, 91.5f, -12.25f);
        SpawnStore store = new SpawnStore(directory.resolve("spawn.properties"));

        store.save(expected);

        assertEquals(expected, store.load().orElseThrow());
    }

    @Test
    void missingFileHasNoSpawn() throws Exception {
        assertTrue(new SpawnStore(directory.resolve("missing.properties")).load().isEmpty());
    }

    @Test
    void malformedDataIsRejected() throws Exception {
        Path path = directory.resolve("spawn.properties");
        Files.writeString(path, "dimension=minecraft:overworld\nx=NaN\ny=64\nz=0\nyaw=0\npitch=0\n");

        org.junit.jupiter.api.Assertions.assertThrows(
                IOException.class, () -> new SpawnStore(path).load());
    }
}

package f1nal.essentials.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HomeStoreTest {
    @TempDir Path directory;

    @Test
    void roundTripsOwnerDestinationAndRotationAcrossReload() throws Exception {
        UUID owner = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        StoredHome home = new StoredHome(owner, "Cottage", new HomePoint(
                "minecraft:the_end", -8.25, 63, 40.5, 12.5f, 33.25f));
        Path path = directory.resolve("homes.properties");

        new HomeStore(path).save(List.of(home));
        String text = Files.readString(path);
        assertTrue(text.contains(owner.toString()));
        assertFalse(text.contains("OldName"));

        List<StoredHome> loaded = new HomeStore(path).load();
        assertEquals(1, loaded.size());
        assertEquals(home.owner(), loaded.get(0).owner());
        assertEquals("cottage", loaded.get(0).name());
        assertEquals(home.point(), loaded.get(0).point());
    }

    @Test
    void missingFileIsEmpty() throws Exception {
        assertTrue(new HomeStore(directory.resolve("missing.properties")).load().isEmpty());
    }

    @Test
    void malformedEntryIsSkippedAndValidEntryRemains() throws Exception {
        UUID owner = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
        Path path = directory.resolve("homes.properties");
        Files.writeString(path, """
                count=2
                home.0.owner=not-a-uuid
                home.0.name=broken
                home.0.dimension=minecraft:overworld
                home.0.x=NaN
                home.0.y=64
                home.0.z=0
                home.0.yaw=0
                home.0.pitch=0
                home.1.owner=%s
                home.1.name=Home
                home.1.dimension=minecraft:overworld
                home.1.x=1.5
                home.1.y=70
                home.1.z=-2
                home.1.yaw=45
                home.1.pitch=-5
                """.formatted(owner));

        List<StoredHome> loaded = new HomeStore(path).load();
        assertEquals(1, loaded.size());
        assertEquals(owner, loaded.get(0).owner());
        assertEquals("home", loaded.get(0).name());
        assertEquals(1.5, loaded.get(0).point().x());
        assertEquals(45f, loaded.get(0).point().yaw());
    }
}

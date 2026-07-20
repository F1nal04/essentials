package f1nal.essentials.vanish;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VanishStoreTest {
    @TempDir Path directory;

    @Test
    void roundTripsUuidState() throws Exception {
        VanishStore store = new VanishStore(directory.resolve("vanished.properties"));
        Set<UUID> expected = Set.of(UUID.randomUUID(), UUID.randomUUID());
        store.save(expected);
        assertEquals(expected, store.load());
    }

    @Test
    void malformedEntriesDoNotDiscardValidState() throws Exception {
        Path path = directory.resolve("vanished.properties");
        UUID valid = UUID.randomUUID();
        Files.writeString(path, valid + "=true\nnot-a-uuid=true\n"
                + UUID.randomUUID() + "=false\n");
        assertEquals(Set.of(valid), new VanishStore(path).load());
    }

    @Test
    void missingFileIsEmpty() throws Exception {
        assertEquals(Set.of(), new VanishStore(directory.resolve("missing.properties")).load());
    }
}

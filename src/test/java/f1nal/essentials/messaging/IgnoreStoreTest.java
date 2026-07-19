package f1nal.essentials.messaging;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IgnoreStoreTest {
    @TempDir
    Path directory;

    @Test
    void relationshipsAreDirectedAndSurviveReload() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID ignored = UUID.randomUUID();
        Path file = directory.resolve("ignored.properties");
        IgnoreStore store = new IgnoreStore(file);

        assertTrue(store.toggle(owner, ignored));
        assertTrue(store.isIgnoring(owner, ignored));
        assertFalse(store.isIgnoring(ignored, owner));

        IgnoreStore reloaded = new IgnoreStore(file);
        reloaded.load();
        assertTrue(reloaded.isIgnoring(owner, ignored));
        assertFalse(reloaded.toggle(owner, ignored));

        IgnoreStore afterRemoval = new IgnoreStore(file);
        afterRemoval.load();
        assertFalse(afterRemoval.isIgnoring(owner, ignored));
    }
}

package f1nal.essentials.vanish;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

/** Durable set of vanished UUIDs. Malformed hand-edited entries are ignored. */
public final class VanishStore {
    private final Path path;

    public VanishStore(Path path) {
        this.path = path;
    }

    public Set<UUID> load() throws IOException {
        Set<UUID> result = new HashSet<>();
        if (!Files.exists(path)) return result;
        Properties values = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            values.load(input);
        }
        for (String key : values.stringPropertyNames()) {
            if (!Boolean.parseBoolean(values.getProperty(key))) continue;
            try {
                result.add(UUID.fromString(key));
            } catch (IllegalArgumentException ignored) {
                // Preserve valid entries when one line is malformed.
            }
        }
        return result;
    }

    public void save(Set<UUID> vanished) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Properties values = new Properties();
        for (UUID playerId : vanished) values.setProperty(playerId.toString(), "true");
        Path temporary = Files.createTempFile(parent, "vanished-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                values.store(output, "Essentials vanished players (UUIDs)");
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}

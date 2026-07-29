package f1nal.essentials.spawn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Properties;

/** Atomic, world-scoped persistence for the configured Essentials spawn. */
public final class SpawnStore {
    private final Path path;

    public SpawnStore(Path path) {
        this.path = path;
    }

    public Optional<SpawnLocation> load() throws IOException {
        if (!Files.exists(path)) return Optional.empty();
        Properties values = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            values.load(input);
        }
        try {
            return Optional.of(new SpawnLocation(
                    required(values, "dimension"),
                    Double.parseDouble(required(values, "x")),
                    Double.parseDouble(required(values, "y")),
                    Double.parseDouble(required(values, "z")),
                    Float.parseFloat(required(values, "yaw")),
                    Float.parseFloat(required(values, "pitch"))));
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid spawn data in " + path, e);
        }
    }

    public void save(SpawnLocation location) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Properties values = new Properties();
        values.setProperty("dimension", location.dimension());
        values.setProperty("x", Double.toString(location.x()));
        values.setProperty("y", Double.toString(location.y()));
        values.setProperty("z", Double.toString(location.z()));
        values.setProperty("yaw", Float.toString(location.yaw()));
        values.setProperty("pitch", Float.toString(location.pitch()));

        Path temporary = Files.createTempFile(parent, "spawn-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                values.store(output, "Essentials server spawn");
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

    private static String required(Properties values, String key) {
        String value = values.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing " + key);
        }
        return value.trim();
    }
}

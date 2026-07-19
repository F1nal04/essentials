package f1nal.essentials.messaging;

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

/** Durable directed ignore relationships, keyed exclusively by UUID. */
public final class IgnoreStore {
    private final Path path;
    private final Set<Relationship> relationships = new HashSet<>();

    public IgnoreStore(Path path) {
        this.path = path;
    }

    public synchronized void load() throws IOException {
        relationships.clear();
        if (!Files.exists(path)) return;
        Properties values = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            values.load(input);
        }
        for (String key : values.stringPropertyNames()) {
            String[] ids = key.split(":", -1);
            if (ids.length != 2 || !Boolean.parseBoolean(values.getProperty(key))) continue;
            try {
                relationships.add(new Relationship(UUID.fromString(ids[0]), UUID.fromString(ids[1])));
            } catch (IllegalArgumentException ignored) {
                // Keep valid relationships even if one hand-edited line is malformed.
            }
        }
    }

    public synchronized boolean isIgnoring(UUID owner, UUID ignored) {
        return relationships.contains(new Relationship(owner, ignored));
    }

    /** Toggles and durably saves. The in-memory change is rolled back if saving fails. */
    public synchronized boolean toggle(UUID owner, UUID ignored) throws IOException {
        Relationship relationship = new Relationship(owner, ignored);
        boolean nowIgnoring;
        if (relationships.remove(relationship)) {
            nowIgnoring = false;
        } else {
            relationships.add(relationship);
            nowIgnoring = true;
        }
        try {
            save();
            return nowIgnoring;
        } catch (IOException e) {
            if (nowIgnoring) relationships.remove(relationship);
            else relationships.add(relationship);
            throw e;
        }
    }

    private void save() throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent());
        Properties values = new Properties();
        for (Relationship relationship : relationships) {
            values.setProperty(relationship.owner + ":" + relationship.ignored, "true");
        }
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), "ignored-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                values.store(output, "Essentials ignored players (UUID pairs)");
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

    private record Relationship(UUID owner, UUID ignored) {
    }
}

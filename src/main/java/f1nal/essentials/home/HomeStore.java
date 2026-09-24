package f1nal.essentials.home;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/** Atomic, world-scoped persistence for UUID-owned homes. Malformed entries are skipped. */
public final class HomeStore {
    private final Path path;

    public HomeStore(Path path) {
        this.path = path;
    }

    public List<StoredHome> load() throws IOException {
        if (!Files.exists(path)) return List.of();
        Properties values = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            values.load(input);
        }
        int count = parseCount(values);
        List<StoredHome> homes = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            StoredHome home = read(values, index);
            if (home != null) homes.add(home);
        }
        return List.copyOf(homes);
    }

    public void save(List<StoredHome> homes) throws IOException {
        List<StoredHome> ordered = new ArrayList<>(homes);
        ordered.sort(Comparator.comparing(StoredHome::owner).thenComparing(StoredHome::name));
        Path parent = path.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Properties values = new Properties();
        values.setProperty("count", Integer.toString(ordered.size()));
        for (int index = 0; index < ordered.size(); index++) {
            write(values, index, ordered.get(index));
        }

        Path temporary = Files.createTempFile(parent, "homes-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                values.store(output, "Essentials player homes (UUID-owned)");
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

    private static void write(Properties values, int index, StoredHome home) {
        String prefix = "home." + index + ".";
        HomePoint point = home.point();
        values.setProperty(prefix + "owner", home.owner().toString());
        values.setProperty(prefix + "name", home.name());
        values.setProperty(prefix + "dimension", point.dimension());
        values.setProperty(prefix + "x", Double.toString(point.x()));
        values.setProperty(prefix + "y", Double.toString(point.y()));
        values.setProperty(prefix + "z", Double.toString(point.z()));
        values.setProperty(prefix + "yaw", Float.toString(point.yaw()));
        values.setProperty(prefix + "pitch", Float.toString(point.pitch()));
    }

    private static int parseCount(Properties values) {
        String raw = values.getProperty("count");
        if (raw != null) {
            try {
                int parsed = Integer.parseInt(raw.trim());
                if (parsed >= 0) return parsed;
            } catch (NumberFormatException ignored) {
                // Fall through and discover indexed entries.
            }
        }
        int max = -1;
        for (String key : values.stringPropertyNames()) {
            if (!key.startsWith("home.") || !key.endsWith(".owner")) continue;
            String index = key.substring("home.".length(), key.length() - ".owner".length());
            try {
                max = Math.max(max, Integer.parseInt(index));
            } catch (NumberFormatException ignored) {
                // Ignore keys that are not home indexes.
            }
        }
        return max + 1;
    }

    private static StoredHome read(Properties values, int index) {
        String prefix = "home." + index + ".";
        try {
            String owner = values.getProperty(prefix + "owner");
            String name = values.getProperty(prefix + "name");
            String dimension = values.getProperty(prefix + "dimension");
            if (owner == null || name == null || dimension == null) return null;
            HomePoint point = new HomePoint(
                    dimension,
                    Double.parseDouble(required(values, prefix + "x")),
                    Double.parseDouble(required(values, prefix + "y")),
                    Double.parseDouble(required(values, prefix + "z")),
                    Float.parseFloat(required(values, prefix + "yaw")),
                    Float.parseFloat(required(values, prefix + "pitch")));
            return new StoredHome(UUID.fromString(owner.trim()), name, point);
        } catch (RuntimeException ignored) {
            return null;
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

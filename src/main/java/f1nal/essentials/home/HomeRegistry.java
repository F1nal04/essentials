package f1nal.essentials.home;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/** In-memory homes keyed by owner UUID and canonical name. */
public final class HomeRegistry {
    public enum SetStatus { CREATED, DUPLICATE, LIMIT_REACHED }
    public enum DeleteStatus { DELETED, MISSING }

    private final Map<UUID, TreeMap<String, HomePoint>> homes = new HashMap<>();

    public SetStatus set(UUID owner, String name, HomePoint point, int limit) {
        if (owner == null) throw new IllegalArgumentException("owner must not be null");
        if (point == null) throw new IllegalArgumentException("point must not be null");
        String canonical = HomeNames.canonical(name);
        if (canonical.isEmpty()) throw new IllegalArgumentException("name must not be blank");
        TreeMap<String, HomePoint> owned = homes.computeIfAbsent(owner, id -> new TreeMap<>());
        if (owned.containsKey(canonical)) return SetStatus.DUPLICATE;
        if (!HomeLimits.canCreate(owned.size(), limit)) {
            if (owned.isEmpty()) homes.remove(owner);
            return SetStatus.LIMIT_REACHED;
        }
        owned.put(canonical, point);
        return SetStatus.CREATED;
    }

    public DeleteStatus delete(UUID owner, String name) {
        if (owner == null) return DeleteStatus.MISSING;
        TreeMap<String, HomePoint> owned = homes.get(owner);
        if (owned == null || owned.remove(HomeNames.canonical(name)) == null) {
            return DeleteStatus.MISSING;
        }
        if (owned.isEmpty()) homes.remove(owner);
        return DeleteStatus.DELETED;
    }

    public Optional<HomePoint> find(UUID owner, String name) {
        if (owner == null) return Optional.empty();
        TreeMap<String, HomePoint> owned = homes.get(owner);
        if (owned == null) return Optional.empty();
        return Optional.ofNullable(owned.get(HomeNames.canonical(name)));
    }

    public List<String> names(UUID owner) {
        TreeMap<String, HomePoint> owned = homes.get(owner);
        if (owned == null) return List.of();
        return List.copyOf(owned.keySet());
    }

    public int count(UUID owner) {
        TreeMap<String, HomePoint> owned = homes.get(owner);
        return owned == null ? 0 : owned.size();
    }

    public void replace(List<StoredHome> loaded) {
        homes.clear();
        if (loaded == null) return;
        for (StoredHome home : loaded) {
            homes.computeIfAbsent(home.owner(), id -> new TreeMap<>())
                    .putIfAbsent(home.name(), home.point());
        }
    }

    public List<StoredHome> entries() {
        List<StoredHome> result = new ArrayList<>();
        for (Map.Entry<UUID, TreeMap<String, HomePoint>> owner : homes.entrySet()) {
            for (Map.Entry<String, HomePoint> home : owner.getValue().entrySet()) {
                result.add(new StoredHome(owner.getKey(), home.getKey(), home.getValue()));
            }
        }
        result.sort(Comparator.comparing(StoredHome::owner).thenComparing(StoredHome::name));
        return result;
    }
}

package f1nal.essentials.messaging;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Shared vanish-state boundary used by target-resolving Essentials features. */
public final class VanishVisibility {
    private static final Set<UUID> VANISHED = ConcurrentHashMap.newKeySet();

    private VanishVisibility() {
    }

    public static boolean isVanished(UUID playerId) {
        return VANISHED.contains(playerId);
    }

    public static void setVanished(UUID playerId, boolean vanished) {
        if (vanished) VANISHED.add(playerId);
        else VANISHED.remove(playerId);
    }

    public static void replace(Set<UUID> playerIds) {
        VANISHED.clear();
        VANISHED.addAll(playerIds);
    }

    public static Set<UUID> snapshot() {
        return Set.copyOf(VANISHED);
    }

    public static void clear() {
        VANISHED.clear();
    }
}

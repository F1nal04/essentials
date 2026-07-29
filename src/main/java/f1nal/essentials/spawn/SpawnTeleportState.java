package f1nal.essentials.spawn;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;

/** Pure warm-up, cooldown, movement, and damage state for spawn teleports. */
public final class SpawnTeleportState {
    private static final double MOVEMENT_TOLERANCE_SQUARED = 0.01;

    private final LongSupplier clock;
    private final Map<UUID, Pending> pending = new HashMap<>();
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();

    public SpawnTeleportState(LongSupplier clock) {
        this.clock = clock;
    }

    public Request precheck(UUID playerId, boolean bypassCooldown) {
        removeExpiredCooldowns();
        if (pending.containsKey(playerId)) return new Request(RequestResult.ALREADY_PENDING, 0);
        long remaining = cooldownUntil.getOrDefault(playerId, 0L) - clock.getAsLong();
        if (!bypassCooldown && remaining > 0) {
            return new Request(RequestResult.COOLDOWN, secondsCeil(remaining));
        }
        return new Request(RequestResult.READY, 0);
    }

    public int removeExpiredCooldowns() {
        long now = clock.getAsLong();
        int before = cooldownUntil.size();
        cooldownUntil.values().removeIf(until -> until <= now);
        return before - cooldownUntil.size();
    }

    public Request request(UUID playerId, Origin origin, long warmupMs,
            boolean bypassCooldown) {
        Request pre = precheck(playerId, bypassCooldown);
        if (pre.result() != RequestResult.READY) return pre;
        if (warmupMs <= 0) return new Request(RequestResult.READY, 0);
        pending.put(playerId, new Pending(origin, clock.getAsLong() + warmupMs));
        return new Request(RequestResult.WARMING_UP, secondsCeil(warmupMs));
    }

    public TickResult tick(UUID playerId, Origin current, boolean cancelOnMovement) {
        Pending value = pending.get(playerId);
        if (value == null) return TickResult.NONE;
        if (cancelOnMovement && value.origin().movedFrom(current)) {
            pending.remove(playerId);
            return TickResult.MOVED;
        }
        if (clock.getAsLong() >= value.readyAt()) {
            pending.remove(playerId);
            return TickResult.READY;
        }
        return TickResult.NONE;
    }

    public boolean damage(UUID playerId, boolean cancelOnDamage) {
        return cancelOnDamage && pending.remove(playerId) != null;
    }

    public void complete(UUID playerId, long cooldownMs) {
        if (cooldownMs > 0) cooldownUntil.put(playerId, clock.getAsLong() + cooldownMs);
    }

    public void cancelPending(UUID playerId) {
        pending.remove(playerId);
    }

    public void clearAll() {
        pending.clear();
        cooldownUntil.clear();
    }

    public Set<UUID> pendingPlayers() {
        return Set.copyOf(pending.keySet());
    }

    private static long secondsCeil(long milliseconds) {
        return Math.max(1, (milliseconds + 999) / 1000);
    }

    public enum RequestResult { READY, WARMING_UP, COOLDOWN, ALREADY_PENDING }
    public enum TickResult { NONE, READY, MOVED }

    public record Request(RequestResult result, long seconds) {
    }

    private record Pending(Origin origin, long readyAt) {
    }

    public record Origin(String dimension, double x, double y, double z) {
        boolean movedFrom(Origin other) {
            if (!dimension.equals(other.dimension)) return true;
            double dx = x - other.x;
            double dy = y - other.y;
            double dz = z - other.z;
            return dx * dx + dy * dy + dz * dz > MOVEMENT_TOLERANCE_SQUARED;
        }
    }
}

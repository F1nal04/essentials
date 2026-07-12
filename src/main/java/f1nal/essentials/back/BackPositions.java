package f1nal.essentials.back;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Expiring per-player position store backing /back. Pure JVM (no Fabric or
 * Minecraft imports) so it stays unit testable off-game.
 */
public final class BackPositions<P> {

    private record Stamped<P>(P position, long expiresAtMillis) {

        boolean isExpired(long now) {
            return now >= expiresAtMillis;
        }
    }

    private final LongSupplier clock;
    private final LongSupplier windowMillis;
    private final Map<UUID, Stamped<P>> entries = new ConcurrentHashMap<>();

    public BackPositions(LongSupplier clock, LongSupplier windowMillis) {
        this.clock = clock;
        this.windowMillis = windowMillis;
    }

    public void mark(UUID player, P position) {
        entries.put(player, new Stamped<>(position, clock.getAsLong() + windowMillis.getAsLong()));
    }

    public Optional<P> peek(UUID player) {
        cleanup();
        Stamped<P> e = entries.get(player);
        return e == null ? Optional.empty() : Optional.of(e.position());
    }

    public Optional<P> consume(UUID player) {
        cleanup();
        Stamped<P> e = entries.remove(player);
        return e == null ? Optional.empty() : Optional.of(e.position());
    }

    public void cleanup() {
        long now = clock.getAsLong();
        entries.entrySet().removeIf(en -> en.getValue().isExpired(now));
    }
}

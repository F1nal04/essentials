package f1nal.essentials.tpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import org.jetbrains.annotations.Nullable;

/**
 * TPA/TPAHere request state machine with expiry and cancel cooldowns, keyed
 * by player UUID. Pure JVM (no Fabric or Minecraft imports) so it stays unit
 * testable off-game; {@link TpaManager} adapts Minecraft types onto it.
 *
 * A sender may have one pending "batch" of outgoing requests at a time:
 * a single request for /tpa and /tpahere, or one request per online player
 * for /tpahere all. Targets accept or deny their own request independently;
 * /tpcancel withdraws the whole batch.
 */
public final class TpaRequests {

    public enum Type {
        TPA,
        TPA_HERE
    }

    public static final class Request {
        public final UUID sender;
        public final UUID target;
        public final Type type;
        public final long createdAtMillis;
        public final long expiresAtMillis;

        public Request(UUID sender, UUID target, Type type, long createdAtMillis, long expiresAtMillis) {
            this.sender = sender;
            this.target = target;
            this.type = type;
            this.createdAtMillis = createdAtMillis;
            this.expiresAtMillis = expiresAtMillis;
        }

        public boolean isExpired(long now) {
            return now >= expiresAtMillis;
        }
    }

    private final LongSupplier clock;
    private final LongSupplier ttlMillis;
    private final LongSupplier cooldownMillis;

    // One outgoing batch per sender at a time (single request, or one per player for "tpahere all")
    private final Map<UUID, List<Request>> outgoingBySender = new ConcurrentHashMap<>();
    // Incoming requests per target (may be multiple senders)
    private final Map<UUID, List<Request>> incomingByTarget = new ConcurrentHashMap<>();
    // Cancel cooldown per sender
    private final Map<UUID, Long> cancelCooldownUntil = new ConcurrentHashMap<>();

    public TpaRequests(LongSupplier clock, LongSupplier ttlMillis, LongSupplier cooldownMillis) {
        this.clock = clock;
        this.ttlMillis = ttlMillis;
        this.cooldownMillis = cooldownMillis;
    }

    public synchronized Optional<Long> secondsLeftOnCancelCooldown(UUID sender) {
        long now = clock.getAsLong();
        Long until = cancelCooldownUntil.get(sender);
        if (until == null) return Optional.empty();
        if (until <= now) {
            cancelCooldownUntil.remove(sender);
            return Optional.empty();
        }
        long seconds = (until - now + 999) / 1000;
        return Optional.of(seconds);
    }

    public synchronized boolean createRequest(UUID sender, UUID target, Type type) {
        cleanupExpired();

        if (outgoingBySender.containsKey(sender)) {
            return false;
        }

        long now = clock.getAsLong();
        Long cooldown = cancelCooldownUntil.get(sender);
        if (cooldown != null && cooldown > now) {
            return false;
        }

        addRequest(sender, target, type, now);
        return true;
    }

    /**
     * Creates one request per target as a single batch. Returns the number of
     * requests created, or -1 if the sender already has a pending batch or is
     * on cancel cooldown. Targets equal to the sender are skipped.
     */
    public synchronized int createRequests(UUID sender, List<UUID> targets, Type type) {
        cleanupExpired();

        if (outgoingBySender.containsKey(sender)) {
            return -1;
        }

        long now = clock.getAsLong();
        Long cooldown = cancelCooldownUntil.get(sender);
        if (cooldown != null && cooldown > now) {
            return -1;
        }

        int count = 0;
        for (UUID target : targets) {
            if (target.equals(sender)) continue;
            addRequest(sender, target, type, now);
            count++;
        }
        return count;
    }

    private void addRequest(UUID sender, UUID target, Type type, long now) {
        Request req = new Request(sender, target, type, now, now + ttlMillis.getAsLong());
        outgoingBySender.computeIfAbsent(sender, k -> new ArrayList<>()).add(req);
        incomingByTarget.computeIfAbsent(target, k -> new ArrayList<>()).add(req);
    }

    public synchronized Optional<Request> findIncomingFor(UUID target, @Nullable UUID from) {
        cleanupExpired();
        List<Request> list = incomingByTarget.getOrDefault(target, Collections.emptyList());
        if (list.isEmpty()) return Optional.empty();
        if (from == null) {
            return list.stream().max(Comparator.comparingLong(r -> r.createdAtMillis));
        }
        return list.stream().filter(r -> r.sender.equals(from)).findFirst();
    }

    public synchronized Optional<Request> accept(UUID target, @Nullable UUID from) {
        Optional<Request> reqOpt = findIncomingFor(target, from);
        reqOpt.ifPresent(this::remove);
        return reqOpt;
    }

    public synchronized Optional<Request> deny(UUID target, @Nullable UUID from) {
        Optional<Request> reqOpt = findIncomingFor(target, from);
        reqOpt.ifPresent(this::remove);
        return reqOpt;
    }

    public synchronized boolean cancel(UUID sender) {
        cleanupExpired();
        List<Request> batch = outgoingBySender.remove(sender);
        if (batch == null || batch.isEmpty()) return false;
        for (Request req : batch) {
            List<Request> list = incomingByTarget.get(req.target);
            if (list != null) {
                list.remove(req);
                if (list.isEmpty()) incomingByTarget.remove(req.target);
            }
        }
        cancelCooldownUntil.put(sender, clock.getAsLong() + cooldownMillis.getAsLong());
        return true;
    }

    /** Removes every request involving a player without applying cancel cooldown. */
    public synchronized void removeAllFor(UUID playerId) {
        List<Request> matches = new ArrayList<>();
        for (List<Request> requests : outgoingBySender.values()) {
            for (Request request : requests) {
                if (request.sender.equals(playerId) || request.target.equals(playerId)) {
                    matches.add(request);
                }
            }
        }
        matches.forEach(this::remove);
    }

    private void remove(Request req) {
        List<Request> outgoing = outgoingBySender.get(req.sender);
        if (outgoing != null) {
            outgoing.remove(req);
            if (outgoing.isEmpty()) outgoingBySender.remove(req.sender);
        }
        List<Request> incoming = incomingByTarget.get(req.target);
        if (incoming != null) {
            incoming.remove(req);
            if (incoming.isEmpty()) incomingByTarget.remove(req.target);
        }
    }

    public synchronized void cleanupExpired() {
        long now = clock.getAsLong();
        // Outgoing
        Iterator<Map.Entry<UUID, List<Request>>> it = outgoingBySender.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, List<Request>> e = it.next();
            e.getValue().removeIf(r -> r.isExpired(now));
            if (e.getValue().isEmpty()) it.remove();
        }
        // Incoming
        Iterator<Map.Entry<UUID, List<Request>>> it2 = incomingByTarget.entrySet().iterator();
        while (it2.hasNext()) {
            Map.Entry<UUID, List<Request>> e = it2.next();
            e.getValue().removeIf(r -> r.isExpired(now));
            if (e.getValue().isEmpty()) it2.remove();
        }
        cancelCooldownUntil.entrySet().removeIf(en -> en.getValue() <= now);
    }
}

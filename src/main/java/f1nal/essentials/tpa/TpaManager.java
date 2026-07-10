package f1nal.essentials.tpa;

import f1nal.essentials.config.TpaConfig;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Manages TPA/TPAHere requests with expiry and cancel cooldowns.
 *
 * A sender may have one pending "batch" of outgoing requests at a time:
 * a single request for /tpa and /tpahere, or one request per online player
 * for /tpahere all. Targets accept or deny their own request independently;
 * /tpcancel withdraws the whole batch.
 */
public final class TpaManager {

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

    // One outgoing batch per sender at a time (single request, or one per player for "tpahere all")
    private static final Map<UUID, List<Request>> outgoingBySender = new ConcurrentHashMap<>();
    // Incoming requests per target (may be multiple senders)
    private static final Map<UUID, List<Request>> incomingByTarget = new ConcurrentHashMap<>();
    // Cancel cooldown per sender
    private static final Map<UUID, Long> cancelCooldownUntil = new ConcurrentHashMap<>();

    private TpaManager() {}

    private static long requestTtlMillis() {
        return Math.max(1, TpaConfig.get().timeoutSeconds) * 1000L;
    }

    private static long cancelCooldownMillis() {
        int s = TpaConfig.get().cooldownSeconds;
        if (s < 0) s = 0;
        return s * 1000L;
    }

    public static Optional<Long> getSecondsLeftOnCancelCooldown(ServerPlayer sender) {
        long now = System.currentTimeMillis();
        Long until = cancelCooldownUntil.get(sender.getUUID());
        if (until == null) return Optional.empty();
        if (until <= now) {
            cancelCooldownUntil.remove(sender.getUUID());
            return Optional.empty();
        }
        long seconds = (until - now + 999) / 1000;
        return Optional.of(seconds);
    }

    public static synchronized boolean createRequest(ServerPlayer sender, ServerPlayer target, Type type) {
        cleanupExpired();
        UUID s = sender.getUUID();

        if (outgoingBySender.containsKey(s)) {
            return false;
        }

        long now = System.currentTimeMillis();
        Long cooldown = cancelCooldownUntil.get(s);
        if (cooldown != null && cooldown > now) {
            return false;
        }

        addRequest(s, target.getUUID(), type, now);
        return true;
    }

    /**
     * Creates one request per target as a single batch. Returns the number of
     * requests created, or -1 if the sender already has a pending batch or is
     * on cancel cooldown. Targets equal to the sender are skipped.
     */
    public static synchronized int createRequests(ServerPlayer sender, Collection<ServerPlayer> targets, Type type) {
        cleanupExpired();
        UUID s = sender.getUUID();

        if (outgoingBySender.containsKey(s)) {
            return -1;
        }

        long now = System.currentTimeMillis();
        Long cooldown = cancelCooldownUntil.get(s);
        if (cooldown != null && cooldown > now) {
            return -1;
        }

        int count = 0;
        for (ServerPlayer target : targets) {
            UUID t = target.getUUID();
            if (t.equals(s)) continue;
            addRequest(s, t, type, now);
            count++;
        }
        return count;
    }

    private static void addRequest(UUID sender, UUID target, Type type, long now) {
        Request req = new Request(sender, target, type, now, now + requestTtlMillis());
        outgoingBySender.computeIfAbsent(sender, k -> new ArrayList<>()).add(req);
        incomingByTarget.computeIfAbsent(target, k -> new ArrayList<>()).add(req);
    }

    public static synchronized Optional<Request> findIncomingFor(ServerPlayer target, @org.jetbrains.annotations.Nullable ServerPlayer from) {
        cleanupExpired();
        List<Request> list = incomingByTarget.getOrDefault(target.getUUID(), Collections.emptyList());
        if (list.isEmpty()) return Optional.empty();
        if (from == null) {
            return list.stream().max(Comparator.comparingLong(r -> r.createdAtMillis));
        }
        UUID fromId = from.getUUID();
        return list.stream().filter(r -> r.sender.equals(fromId)).findFirst();
    }

    public static synchronized Optional<Request> accept(ServerPlayer target, @org.jetbrains.annotations.Nullable ServerPlayer from) {
        Optional<Request> reqOpt = findIncomingFor(target, from);
        reqOpt.ifPresent(TpaManager::remove);
        return reqOpt;
    }

    public static synchronized Optional<Request> deny(ServerPlayer target, @org.jetbrains.annotations.Nullable ServerPlayer from) {
        Optional<Request> reqOpt = findIncomingFor(target, from);
        reqOpt.ifPresent(TpaManager::remove);
        return reqOpt;
    }

    public static synchronized boolean cancel(ServerPlayer sender) {
        cleanupExpired();
        UUID s = sender.getUUID();
        List<Request> batch = outgoingBySender.remove(s);
        if (batch == null || batch.isEmpty()) return false;
        for (Request req : batch) {
            List<Request> list = incomingByTarget.get(req.target);
            if (list != null) {
                list.remove(req);
                if (list.isEmpty()) incomingByTarget.remove(req.target);
            }
        }
        cancelCooldownUntil.put(s, System.currentTimeMillis() + cancelCooldownMillis());
        return true;
    }

    private static void remove(Request req) {
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

    public static synchronized void cleanupExpired() {
        long now = System.currentTimeMillis();
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

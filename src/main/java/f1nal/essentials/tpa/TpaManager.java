package f1nal.essentials.tpa;

import f1nal.essentials.config.TpaConfig;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages TPA/TPAHere requests with expiry and cancel cooldowns.
 */
public final class TpaManager {

    public enum Type {
        TPA,        // sender wants to go to target
        TPA_HERE    // target should come to sender
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

    // One outgoing request per sender at a time
    private static final Map<UUID, Request> outgoingBySender = new ConcurrentHashMap<>();
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

    public static Optional<Long> getSecondsLeftOnCancelCooldown(ServerPlayerEntity sender) {
        long now = System.currentTimeMillis();
        Long until = cancelCooldownUntil.get(sender.getUuid());
        if (until == null) return Optional.empty();
        if (until <= now) {
            cancelCooldownUntil.remove(sender.getUuid());
            return Optional.empty();
        }
        long seconds = (until - now + 999) / 1000;
        return Optional.of(seconds);
    }

    public static synchronized boolean createRequest(ServerPlayerEntity sender, ServerPlayerEntity target, Type type) {
        cleanupExpired();
        UUID s = sender.getUuid();
        UUID t = target.getUuid();

        // Enforce one pending outgoing per sender
        if (outgoingBySender.containsKey(s)) {
            return false;
        }

        long now = System.currentTimeMillis();
        Long cooldown = cancelCooldownUntil.get(s);
        if (cooldown != null && cooldown > now) {
            return false;
        }

        Request req = new Request(s, t, type, now, now + requestTtlMillis());
        outgoingBySender.put(s, req);
        incomingByTarget.computeIfAbsent(t, k -> new ArrayList<>()).add(req);
        return true;
    }

    public static synchronized Optional<Request> findIncomingFor(ServerPlayerEntity target, @org.jetbrains.annotations.Nullable ServerPlayerEntity from) {
        cleanupExpired();
        List<Request> list = incomingByTarget.getOrDefault(target.getUuid(), Collections.emptyList());
        if (list.isEmpty()) return Optional.empty();
        if (from == null) {
            // return the newest non-expired
            long now = System.currentTimeMillis();
            return list.stream().filter(r -> !r.isExpired(now)).max(Comparator.comparingLong(r -> r.createdAtMillis));
        }
        UUID fromId = from.getUuid();
        return list.stream().filter(r -> r.sender.equals(fromId)).findFirst();
    }

    public static synchronized Optional<Request> accept(ServerPlayerEntity target, @org.jetbrains.annotations.Nullable ServerPlayerEntity from) {
        Optional<Request> reqOpt = findIncomingFor(target, from);
        reqOpt.ifPresent(TpaManager::remove);
        return reqOpt;
    }

    public static synchronized Optional<Request> deny(ServerPlayerEntity target, @org.jetbrains.annotations.Nullable ServerPlayerEntity from) {
        Optional<Request> reqOpt = findIncomingFor(target, from);
        reqOpt.ifPresent(TpaManager::remove);
        return reqOpt;
    }

    public static synchronized boolean cancel(ServerPlayerEntity sender) {
        cleanupExpired();
        UUID s = sender.getUuid();
        Request req = outgoingBySender.remove(s);
        if (req == null) return false;
        List<Request> list = incomingByTarget.get(req.target);
        if (list != null) {
            list.removeIf(r -> r.sender.equals(s));
            if (list.isEmpty()) incomingByTarget.remove(req.target);
        }
        cancelCooldownUntil.put(s, System.currentTimeMillis() + cancelCooldownMillis());
        return true;
    }

    private static void remove(Request req) {
        outgoingBySender.remove(req.sender);
        List<Request> list = incomingByTarget.get(req.target);
        if (list != null) {
            list.removeIf(r -> r.sender.equals(req.sender));
            if (list.isEmpty()) incomingByTarget.remove(req.target);
        }
    }

    public static synchronized void cleanupExpired() {
        long now = System.currentTimeMillis();
        // Outgoing
        Iterator<Map.Entry<UUID, Request>> it = outgoingBySender.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Request> e = it.next();
            if (e.getValue().isExpired(now)) {
                it.remove();
            }
        }
        // Incoming
        Iterator<Map.Entry<UUID, List<Request>>> it2 = incomingByTarget.entrySet().iterator();
        while (it2.hasNext()) {
            Map.Entry<UUID, List<Request>> e = it2.next();
            e.getValue().removeIf(r -> r.isExpired(now));
            if (e.getValue().isEmpty()) it2.remove();
        }
        // Cooldowns cleanup
        cancelCooldownUntil.entrySet().removeIf(en -> en.getValue() <= now);
    }
}

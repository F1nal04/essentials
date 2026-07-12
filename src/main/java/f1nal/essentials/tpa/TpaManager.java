package f1nal.essentials.tpa;

import f1nal.essentials.config.TpaConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;

/**
 * Static facade adapting Minecraft players onto the {@link TpaRequests}
 * state machine, wired to the real clock and {@link TpaConfig}.
 */
public final class TpaManager {

    private static final TpaRequests REQUESTS = new TpaRequests(
            System::currentTimeMillis,
            () -> Math.max(1, TpaConfig.get().timeoutSeconds) * 1000L,
            () -> Math.max(0, TpaConfig.get().cooldownSeconds) * 1000L);

    private TpaManager() {}

    public static Optional<Long> getSecondsLeftOnCancelCooldown(ServerPlayer sender) {
        return REQUESTS.secondsLeftOnCancelCooldown(sender.getUUID());
    }

    public static boolean createRequest(ServerPlayer sender, ServerPlayer target, TpaRequests.Type type) {
        return REQUESTS.createRequest(sender.getUUID(), target.getUUID(), type);
    }

    public static int createRequests(ServerPlayer sender, Collection<ServerPlayer> targets, TpaRequests.Type type) {
        List<UUID> ids = new ArrayList<>(targets.size());
        for (ServerPlayer target : targets) {
            ids.add(target.getUUID());
        }
        return REQUESTS.createRequests(sender.getUUID(), ids, type);
    }

    public static Optional<TpaRequests.Request> accept(ServerPlayer target, @Nullable ServerPlayer from) {
        return REQUESTS.accept(target.getUUID(), from == null ? null : from.getUUID());
    }

    public static Optional<TpaRequests.Request> deny(ServerPlayer target, @Nullable ServerPlayer from) {
        return REQUESTS.deny(target.getUUID(), from == null ? null : from.getUUID());
    }

    public static boolean cancel(ServerPlayer sender) {
        return REQUESTS.cancel(sender.getUUID());
    }
}

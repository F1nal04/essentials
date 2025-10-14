package f1nal.essentials.back;

import f1nal.essentials.config.BackConfig;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players' previous positions for /back after teleports.
 */
public final class BackManager {

    public static final class BackEntry {
        public final RegistryKey<World> worldKey;
        public final double x, y, z;
        public final float yaw, pitch;
        public final long expiresAtMillis;

        public BackEntry(RegistryKey<World> worldKey, double x, double y, double z, float yaw, float pitch, long expiresAtMillis) {
            this.worldKey = worldKey;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.expiresAtMillis = expiresAtMillis;
        }

        public boolean isExpired(long now) {
            return now >= expiresAtMillis;
        }
    }

    private static final Map<UUID, BackEntry> entries = new ConcurrentHashMap<>();

    private BackManager() {}

    private static long windowMillis() {
        return Math.max(1, BackConfig.get().windowSeconds) * 1000L;
    }

    public static void markBackPosition(ServerPlayerEntity player) {
        long now = System.currentTimeMillis();
        BackEntry entry = new BackEntry(
                player.getWorld().getRegistryKey(),
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(),
                now + windowMillis()
        );
        entries.put(player.getUuid(), entry);
    }

    public static Optional<BackEntry> peek(ServerPlayerEntity player) {
        cleanup();
        BackEntry e = entries.get(player.getUuid());
        if (e == null) return Optional.empty();
        if (e.isExpired(System.currentTimeMillis())) {
            entries.remove(player.getUuid());
            return Optional.empty();
        }
        return Optional.of(e);
    }

    public static Optional<BackEntry> consume(ServerPlayerEntity player) {
        cleanup();
        UUID id = player.getUuid();
        BackEntry e = entries.get(id);
        if (e == null) return Optional.empty();
        if (e.isExpired(System.currentTimeMillis())) {
            entries.remove(id);
            return Optional.empty();
        }
        entries.remove(id);
        return Optional.of(e);
    }

    public static void cleanup() {
        long now = System.currentTimeMillis();
        entries.entrySet().removeIf(en -> en.getValue().isExpired(now));
    }

    public static boolean teleportBack(ServerPlayerEntity player) {
        Optional<BackEntry> opt = consume(player);
        if (opt.isEmpty()) return false;
        BackEntry e = opt.get();
        MinecraftServer server = player.getServer();
        if (server == null) return false;
        ServerWorld world = server.getWorld(e.worldKey);
        if (world == null) return false;
        player.teleport(world, e.x, e.y, e.z, java.util.Set.of(), e.yaw, e.pitch, false);
        return true;
    }
}

package f1nal.essentials.back;

import f1nal.essentials.config.BackConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import java.util.Optional;

/**
 * Tracks players' previous positions for /back after teleports. Expiry lives
 * in {@link BackPositions}; this class only adapts Minecraft types.
 */
public final class BackManager {

    public static final class BackEntry {
        public final ResourceKey<Level> worldKey;
        public final double x, y, z;
        public final float yaw, pitch;

        public BackEntry(ResourceKey<Level> worldKey, double x, double y, double z, float yaw, float pitch) {
            this.worldKey = worldKey;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private static final BackPositions<BackEntry> POSITIONS = new BackPositions<>(
            System::currentTimeMillis,
            () -> Math.max(1, BackConfig.get().windowSeconds) * 1000L);

    private BackManager() {}

    public static void markBackPosition(ServerPlayer player) {
        POSITIONS.mark(player.getUUID(), new BackEntry(
                player.level().dimension(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot()));
    }

    public static Optional<BackEntry> peek(ServerPlayer player) {
        return POSITIONS.peek(player.getUUID());
    }

    public static Optional<BackEntry> consume(ServerPlayer player) {
        return POSITIONS.consume(player.getUUID());
    }

    public static boolean teleportBack(ServerPlayer player) {
        Optional<BackEntry> opt = consume(player);
        if (opt.isEmpty()) return false;
        BackEntry e = opt.get();
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        ServerLevel world = server.getLevel(e.worldKey);
        if (world == null) return false;
        player.teleportTo(world, e.x, e.y, e.z, java.util.Set.of(), e.yaw, e.pitch, false);
        return true;
    }
}

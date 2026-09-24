package f1nal.essentials.home;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import f1nal.essentials.Essentials;
import f1nal.essentials.config.HomeConfig;
import f1nal.essentials.permission.EssentialsPermissions;
import f1nal.essentials.spawn.SafePositionFinder;
import f1nal.essentials.spawn.SafePositionFinder.Position;
import f1nal.essentials.spawn.SpawnTeleportState;
import f1nal.essentials.spawn.SpawnTeleportState.Origin;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

/** Lifecycle facade for persistent, UUID-owned player homes. */
public final class HomeManager {
    private static final Set<Block> DANGEROUS_BLOCKS = Set.of(
            Blocks.LAVA, Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.CACTUS,
            Blocks.MAGMA_BLOCK, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE,
            Blocks.SWEET_BERRY_BUSH, Blocks.WITHER_ROSE,
            Blocks.POWDER_SNOW, Blocks.POINTED_DRIPSTONE);

    private static final SpawnTeleportState TELEPORTS =
            new SpawnTeleportState(System::currentTimeMillis);
    private static final Map<UUID, String> PENDING_NAMES = new HashMap<>();

    private static MinecraftServer server;
    private static HomeStore store;
    private static final HomeRegistry REGISTRY = new HomeRegistry();

    private HomeManager() {
    }

    public static synchronized void initialize(MinecraftServer minecraftServer) {
        server = minecraftServer;
        Path worldRoot = minecraftServer.getWorldPath(LevelResource.ROOT);
        store = new HomeStore(worldRoot.resolve("essentials").resolve("homes.properties"));
        try {
            REGISTRY.replace(store.load());
        } catch (IOException e) {
            REGISTRY.replace(List.of());
            Essentials.LOGGER.error("Failed to load player homes", e);
        }
    }

    public static synchronized void close() {
        TELEPORTS.clearAll();
        PENDING_NAMES.clear();
        REGISTRY.replace(List.of());
        store = null;
        server = null;
    }

    public static int limitFor(CommandSourceStack source) {
        HomeConfig config = HomeConfig.get();
        boolean unlimited = EssentialsPermissions.require(
                "home.limit.unlimited",
                Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).test(source);
        return HomeLimits.resolve(config.defaultLimit, config.maximumLimit, unlimited,
                number -> EssentialsPermissions.require(
                        "home.limit." + number, ignored -> false).test(source));
    }

    public static synchronized List<String> names(UUID owner) {
        return REGISTRY.names(owner);
    }

    public static synchronized SetStatus setHome(ServerPlayer player, String name, int limit) {
        if (store == null) return SetStatus.SAVE_FAILED;
        String canonical = HomeNames.canonical(name);
        SetStatus status = switch (REGISTRY.set(player.getUUID(), canonical, capture(player), limit)) {
            case CREATED -> SetStatus.CREATED;
            case DUPLICATE -> SetStatus.DUPLICATE;
            case LIMIT_REACHED -> SetStatus.LIMIT_REACHED;
        };
        if (status != SetStatus.CREATED) return status;
        if (!persist()) {
            REGISTRY.delete(player.getUUID(), canonical);
            return SetStatus.SAVE_FAILED;
        }
        return SetStatus.CREATED;
    }

    public static synchronized DeleteStatus deleteHome(ServerPlayer player, String name) {
        if (store == null) return DeleteStatus.SAVE_FAILED;
        String canonical = HomeNames.canonical(name);
        Optional<HomePoint> existing = REGISTRY.find(player.getUUID(), canonical);
        if (existing.isEmpty()) return DeleteStatus.MISSING;
        REGISTRY.delete(player.getUUID(), canonical);
        if (!persist()) {
            REGISTRY.set(player.getUUID(), canonical, existing.get(), HomeLimits.UNLIMITED);
            return DeleteStatus.SAVE_FAILED;
        }
        return DeleteStatus.DELETED;
    }

    public static Result requestTeleport(ServerPlayer player, String name,
            boolean bypassWarmup, boolean bypassCooldown) {
        String canonical = HomeNames.canonical(name);
        SpawnTeleportState.Request pre = TELEPORTS.precheck(player.getUUID(), bypassCooldown);
        switch (pre.result()) {
            case ALREADY_PENDING -> {
                return new Result(Status.ALREADY_PENDING, canonical, 0);
            }
            case COOLDOWN -> {
                return new Result(Status.COOLDOWN, canonical, pre.seconds());
            }
            default -> {
            }
        }

        HomePoint home = find(player.getUUID(), canonical);
        if (home == null) return new Result(Status.MISSING, canonical, 0);
        HomeConfig config = HomeConfig.get();
        if (HomeTeleportRules.crossDimensionBlocked(
                dimension(player), home.dimension(), config.allowCrossDimension)) {
            return new Result(Status.CROSS_DIMENSION, canonical, 0);
        }
        DestinationResult destination = resolve(player, home);
        if (destination.status() != Status.TELEPORTED) {
            return new Result(destination.status(), canonical, 0);
        }

        long warmupMs = bypassWarmup ? 0L : config.warmupSeconds * 1000L;
        SpawnTeleportState.Request request = TELEPORTS.request(
                player.getUUID(), origin(player), warmupMs, bypassCooldown);
        return switch (request.result()) {
            case ALREADY_PENDING -> new Result(Status.ALREADY_PENDING, canonical, 0);
            case COOLDOWN -> new Result(Status.COOLDOWN, canonical, request.seconds());
            case WARMING_UP -> {
                PENDING_NAMES.put(player.getUUID(), canonical);
                yield new Result(Status.WARMING_UP, canonical, request.seconds());
            }
            case READY -> teleportAndComplete(player, canonical, home, destination);
        };
    }

    public static void tick() {
        MinecraftServer currentServer = server;
        if (currentServer == null) return;
        HomeConfig config = HomeConfig.get();
        for (UUID playerId : TELEPORTS.pendingPlayers()) {
            ServerPlayer player = currentServer.getPlayerList().getPlayer(playerId);
            if (player == null || player.isRemoved()) {
                TELEPORTS.cancelPending(playerId);
                PENDING_NAMES.remove(playerId);
                continue;
            }
            switch (TELEPORTS.tick(playerId, origin(player), config.cancelOnMovement)) {
                case NONE -> {
                }
                case MOVED -> {
                    PENDING_NAMES.remove(playerId);
                    player.sendSystemMessage(HomeMessages.format(config.movementCancelledMessage));
                }
                case READY -> {
                    String pendingName = PENDING_NAMES.remove(playerId);
                    if (pendingName == null) continue;
                    sendResult(player, finishAfterWarmup(player, pendingName));
                }
            }
        }
    }

    public static void onDamage(ServerPlayer player) {
        HomeConfig config = HomeConfig.get();
        if (TELEPORTS.damage(player.getUUID(), config.cancelOnDamage)) {
            PENDING_NAMES.remove(player.getUUID());
            player.sendSystemMessage(HomeMessages.format(config.damageCancelledMessage));
        }
    }

    public static void onDisconnect(ServerPlayer player) {
        TELEPORTS.cancelPending(player.getUUID());
        PENDING_NAMES.remove(player.getUUID());
    }

    public static void sendResult(ServerPlayer player, Result result) {
        HomeConfig config = HomeConfig.get();
        switch (result.status()) {
            case TELEPORTED -> player.sendSystemMessage(HomeMessages.format(
                    config.teleportedMessage, result.name(), null, null, null));
            case WARMING_UP -> player.sendSystemMessage(HomeMessages.format(
                    config.warmupMessage, result.name(), result.seconds(), null, null));
            case COOLDOWN -> player.sendSystemMessage(HomeMessages.format(
                    config.cooldownMessage, null, result.seconds(), null, null));
            case ALREADY_PENDING -> player.sendSystemMessage(
                    HomeMessages.format(config.alreadyPendingMessage));
            case MISSING -> player.sendSystemMessage(HomeMessages.format(
                    config.missingMessage, result.name(), null, null, null));
            case WORLD_UNAVAILABLE -> player.sendSystemMessage(HomeMessages.format(
                    config.unavailableWorldMessage, result.name(), null, null, null));
            case UNSAFE -> player.sendSystemMessage(HomeMessages.format(
                    config.unsafeDestinationMessage, result.name(), null, null, null));
            case CROSS_DIMENSION -> player.sendSystemMessage(
                    HomeMessages.format(config.crossDimensionMessage));
            default -> {
            }
        }
    }

    private static Result finishAfterWarmup(ServerPlayer player, String canonical) {
        HomePoint home = find(player.getUUID(), canonical);
        if (home == null) return new Result(Status.MISSING, canonical, 0);
        HomeConfig config = HomeConfig.get();
        if (HomeTeleportRules.crossDimensionBlocked(
                dimension(player), home.dimension(), config.allowCrossDimension)) {
            return new Result(Status.CROSS_DIMENSION, canonical, 0);
        }
        DestinationResult destination = resolve(player, home);
        if (destination.status() != Status.TELEPORTED) {
            return new Result(destination.status(), canonical, 0);
        }
        return teleportAndComplete(player, canonical, home, destination);
    }

    private static Result teleportAndComplete(ServerPlayer player, String canonical,
            HomePoint home, DestinationResult destination) {
        Result result = teleport(player, canonical, home, destination);
        if (result.status() == Status.TELEPORTED) {
            TELEPORTS.complete(player.getUUID(), HomeConfig.get().cooldownSeconds * 1000L);
        }
        return result;
    }

    private static Result teleport(ServerPlayer player, String canonical, HomePoint home,
            DestinationResult destination) {
        Position position = destination.position().orElseThrow();
        ServerLevel world = destination.world().orElseThrow();
        boolean teleported = player.teleportTo(world,
                position.x(), position.y(), position.z(), Set.of(),
                home.yaw(), home.pitch(), false);
        if (!teleported) return new Result(Status.UNSAFE, canonical, 0);
        player.fallDistance = 0;
        player.clearFire();
        return new Result(Status.TELEPORTED, canonical, 0);
    }

    private static DestinationResult resolve(ServerPlayer player, HomePoint home) {
        MinecraftServer currentServer = server;
        if (currentServer == null) return DestinationResult.failure(Status.WORLD_UNAVAILABLE);
        Identifier identifier = Identifier.tryParse(home.dimension());
        if (identifier == null) return DestinationResult.failure(Status.WORLD_UNAVAILABLE);
        ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, identifier);
        ServerLevel world = currentServer.getLevel(worldKey);
        if (world == null) return DestinationResult.failure(Status.WORLD_UNAVAILABLE);
        Optional<Position> position = SafePositionFinder.find(
                home.x(), home.y(), home.z(),
                candidate -> isSafe(player, world, candidate));
        return position.map(value -> DestinationResult.success(world, value))
                .orElseGet(() -> DestinationResult.failure(Status.UNSAFE));
    }

    private static boolean isSafe(ServerPlayer player, ServerLevel world, Position position) {
        if (position.y() < world.getMinY()
                || position.y() + player.getDimensions(Pose.STANDING).height() > world.getMaxY()) {
            return false;
        }
        AABB box = player.getDimensions(Pose.STANDING).makeBoundingBox(
                position.x(), position.y(), position.z());
        if (!world.getWorldBorder().isWithinBounds(box)
                || !world.noCollision(player, box)) {
            return false;
        }

        BlockPos support = BlockPos.containing(position.x(), position.y() - 0.01, position.z());
        BlockState supportState = world.getBlockState(support);
        if (!supportState.blocksMotion() || isDangerous(supportState)
                || !world.getFluidState(support).isEmpty()) {
            return false;
        }

        BlockPos feet = BlockPos.containing(position.x(), position.y(), position.z());
        BlockPos head = BlockPos.containing(
                position.x(),
                position.y() + player.getDimensions(Pose.STANDING).height() - 0.01,
                position.z());
        return safeBodyBlock(world, feet) && safeBodyBlock(world, head);
    }

    private static boolean safeBodyBlock(ServerLevel world, BlockPos position) {
        return world.getFluidState(position).isEmpty()
                && !isDangerous(world.getBlockState(position));
    }

    private static boolean isDangerous(BlockState state) {
        for (Block block : DANGEROUS_BLOCKS) {
            if (state.is(block)) return true;
        }
        return false;
    }

    private static synchronized HomePoint find(UUID owner, String canonical) {
        return REGISTRY.find(owner, canonical).orElse(null);
    }

    private static boolean persist() {
        try {
            store.save(REGISTRY.entries());
            return true;
        } catch (IOException e) {
            Essentials.LOGGER.error("Failed to save player homes", e);
            return false;
        }
    }

    private static HomePoint capture(ServerPlayer player) {
        return new HomePoint(dimension(player), player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot());
    }

    private static String dimension(ServerPlayer player) {
        return player.level().dimension().identifier().toString();
    }

    private static Origin origin(ServerPlayer player) {
        return new Origin(dimension(player), player.getX(), player.getY(), player.getZ());
    }

    public enum Status {
        TELEPORTED, WARMING_UP, COOLDOWN, ALREADY_PENDING, MISSING,
        WORLD_UNAVAILABLE, UNSAFE, CROSS_DIMENSION
    }

    public enum SetStatus { CREATED, DUPLICATE, LIMIT_REACHED, SAVE_FAILED }

    public enum DeleteStatus { DELETED, MISSING, SAVE_FAILED }

    public record Result(Status status, String name, long seconds) {
    }

    private record DestinationResult(Status status, Optional<ServerLevel> world,
            Optional<Position> position) {
        static DestinationResult success(ServerLevel world, Position position) {
            return new DestinationResult(Status.TELEPORTED, Optional.of(world), Optional.of(position));
        }

        static DestinationResult failure(Status status) {
            return new DestinationResult(status, Optional.empty(), Optional.empty());
        }
    }
}

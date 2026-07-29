package f1nal.essentials.spawn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import f1nal.essentials.Essentials;
import f1nal.essentials.config.SpawnConfig;
import f1nal.essentials.spawn.SafePositionFinder.Position;
import f1nal.essentials.spawn.SpawnTeleportState.Origin;
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

/** Lifecycle facade for the persistent, world-specific Essentials spawn. */
public final class SpawnManager {
    private static final Pattern PLAYER_DATA_FILE = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-"
                    + "[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.dat$");
    private static final Set<Block> DANGEROUS_BLOCKS = Set.of(
            Blocks.LAVA, Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.CACTUS,
            Blocks.MAGMA_BLOCK, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE,
            Blocks.SWEET_BERRY_BUSH, Blocks.WITHER_ROSE,
            Blocks.POWDER_SNOW, Blocks.POINTED_DRIPSTONE);

    private static final SpawnTeleportState TELEPORTS =
            new SpawnTeleportState(System::currentTimeMillis);
    private static final Set<UUID> KNOWN_PLAYERS = new HashSet<>();

    private static MinecraftServer server;
    private static SpawnStore store;
    private static Optional<SpawnLocation> spawn = Optional.empty();

    private SpawnManager() {
    }

    public static synchronized void initialize(MinecraftServer minecraftServer) {
        server = minecraftServer;
        Path worldRoot = minecraftServer.getWorldPath(LevelResource.ROOT);
        store = new SpawnStore(worldRoot.resolve("essentials").resolve("spawn.properties"));
        try {
            spawn = store.load();
        } catch (IOException e) {
            spawn = Optional.empty();
            Essentials.LOGGER.error("Failed to load Essentials spawn", e);
        }
        loadKnownPlayers(minecraftServer.getWorldPath(LevelResource.PLAYER_DATA_DIR));
    }

    public static synchronized void close() {
        TELEPORTS.clearAll();
        KNOWN_PLAYERS.clear();
        spawn = Optional.empty();
        store = null;
        server = null;
    }

    public static synchronized boolean setSpawn(ServerPlayer player) {
        if (store == null) return false;
        SpawnLocation location = new SpawnLocation(
                player.level().dimension().identifier().toString(),
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot());
        try {
            store.save(location);
            spawn = Optional.of(location);
            return true;
        } catch (IOException e) {
            Essentials.LOGGER.error("Failed to save Essentials spawn", e);
            return false;
        }
    }

    public static RequestResult requestTeleport(ServerPlayer player,
            boolean bypassWarmup, boolean bypassCooldown) {
        SpawnTeleportState.Request pre = TELEPORTS.precheck(player.getUUID(), bypassCooldown);
        switch (pre.result()) {
            case ALREADY_PENDING -> {
                return new RequestResult(Status.ALREADY_PENDING, 0);
            }
            case COOLDOWN -> {
                return new RequestResult(Status.COOLDOWN, pre.seconds());
            }
            default -> {
            }
        }

        DestinationResult destination = resolveDestination(player);
        if (destination.status() != Status.TELEPORTED) {
            return new RequestResult(destination.status(), 0);
        }

        SpawnConfig config = SpawnConfig.get();
        long warmupMs = bypassWarmup ? 0L : config.warmupSeconds * 1000L;
        SpawnTeleportState.Request request = TELEPORTS.request(
                player.getUUID(), origin(player), warmupMs, bypassCooldown);
        return switch (request.result()) {
            case ALREADY_PENDING -> new RequestResult(Status.ALREADY_PENDING, 0);
            case COOLDOWN -> new RequestResult(Status.COOLDOWN, request.seconds());
            case WARMING_UP -> new RequestResult(Status.WARMING_UP, request.seconds());
            case READY -> teleportAndComplete(player, destination.position().orElseThrow(),
                    destination.world().orElseThrow());
        };
    }

    public static void tick() {
        MinecraftServer currentServer = server;
        if (currentServer == null) return;
        SpawnConfig config = SpawnConfig.get();
        for (UUID playerId : TELEPORTS.pendingPlayers()) {
            ServerPlayer player = currentServer.getPlayerList().getPlayer(playerId);
            if (player == null || player.isRemoved()) {
                TELEPORTS.cancelPending(playerId);
                continue;
            }
            switch (TELEPORTS.tick(playerId, origin(player), config.cancelOnMovement)) {
                case NONE -> {
                }
                case MOVED -> player.sendSystemMessage(SpawnMessages.format(
                        config.movementCancelledMessage));
                case READY -> {
                    DestinationResult destination = resolveDestination(player);
                    if (destination.status() == Status.TELEPORTED) {
                        RequestResult result = teleportAndComplete(player,
                                destination.position().orElseThrow(),
                                destination.world().orElseThrow());
                        sendResult(player, result);
                    } else {
                        sendResult(player, new RequestResult(destination.status(), 0));
                    }
                }
            }
        }
    }

    public static void onDamage(ServerPlayer player) {
        SpawnConfig config = SpawnConfig.get();
        if (TELEPORTS.damage(player.getUUID(), config.cancelOnDamage)) {
            player.sendSystemMessage(SpawnMessages.format(config.damageCancelledMessage));
        }
    }

    public static void onDisconnect(ServerPlayer player) {
        TELEPORTS.cancelPending(player.getUUID());
    }

    public static void onJoin(ServerPlayer player) {
        boolean firstJoin;
        synchronized (SpawnManager.class) {
            firstJoin = KNOWN_PLAYERS.add(player.getUUID());
        }
        if (SpawnRouting.shouldRouteFirstJoin(
                SpawnConfig.get().firstJoin, firstJoin, spawn.isPresent())) {
            sendResult(player, teleportImmediately(player));
        }
    }

    public static void onRespawn(ServerPlayer player, boolean alive) {
        if (SpawnRouting.shouldRouteRespawn(
                SpawnConfig.get().respawn, alive, spawn.isPresent(),
                player.getRespawnConfig() != null)) {
            sendResult(player, teleportImmediately(player));
        }
    }

    public static void sendResult(ServerPlayer player, RequestResult result) {
        SpawnConfig config = SpawnConfig.get();
        switch (result.status()) {
            case TELEPORTED -> player.sendSystemMessage(
                    SpawnMessages.format(config.teleportedMessage));
            case NO_SPAWN -> player.sendSystemMessage(
                    SpawnMessages.format(config.noSpawnMessage));
            case WORLD_UNAVAILABLE -> player.sendSystemMessage(
                    SpawnMessages.format(config.unavailableWorldMessage));
            case UNSAFE -> player.sendSystemMessage(
                    SpawnMessages.format(config.unsafeDestinationMessage));
            case COOLDOWN -> player.sendSystemMessage(
                    SpawnMessages.format(config.cooldownMessage, result.seconds()));
            case WARMING_UP -> player.sendSystemMessage(
                    SpawnMessages.format(config.warmupMessage, result.seconds()));
            case ALREADY_PENDING -> player.sendSystemMessage(
                    SpawnMessages.format(config.alreadyPendingMessage));
        }
    }

    private static RequestResult teleportImmediately(ServerPlayer player) {
        DestinationResult destination = resolveDestination(player);
        if (destination.status() != Status.TELEPORTED) {
            return new RequestResult(destination.status(), 0);
        }
        return teleport(player, destination.position().orElseThrow(),
                destination.world().orElseThrow());
    }

    private static RequestResult teleportAndComplete(ServerPlayer player,
            Position position, ServerLevel world) {
        RequestResult result = teleport(player, position, world);
        if (result.status() == Status.TELEPORTED) {
            TELEPORTS.complete(player.getUUID(),
                    SpawnConfig.get().cooldownSeconds * 1000L);
        }
        return result;
    }

    private static RequestResult teleport(ServerPlayer player,
            Position position, ServerLevel world) {
        SpawnLocation configured = spawn.orElseThrow();
        boolean teleported = player.teleportTo(world,
                position.x(), position.y(), position.z(), Set.of(),
                configured.yaw(), configured.pitch(), false);
        if (!teleported) return new RequestResult(Status.UNSAFE, 0);
        player.fallDistance = 0;
        player.clearFire();
        return new RequestResult(Status.TELEPORTED, 0);
    }

    private static DestinationResult resolveDestination(ServerPlayer player) {
        SpawnLocation configured = spawn.orElse(null);
        MinecraftServer currentServer = server;
        if (configured == null) return DestinationResult.failure(Status.NO_SPAWN);
        if (currentServer == null) return DestinationResult.failure(Status.WORLD_UNAVAILABLE);

        Identifier identifier = Identifier.tryParse(configured.dimension());
        if (identifier == null) return DestinationResult.failure(Status.WORLD_UNAVAILABLE);
        ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, identifier);
        ServerLevel world = currentServer.getLevel(worldKey);
        if (world == null) return DestinationResult.failure(Status.WORLD_UNAVAILABLE);

        Optional<Position> position = SafePositionFinder.find(
                configured.x(), configured.y(), configured.z(),
                candidate -> isSafe(player, world, candidate));
        return position.map(value -> DestinationResult.success(world, value))
                .orElseGet(() -> DestinationResult.failure(Status.UNSAFE));
    }

    private static boolean isSafe(ServerPlayer player, ServerLevel world,
            Position position) {
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

        BlockPos support = BlockPos.containing(
                position.x(), position.y() - 0.01, position.z());
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

    private static Origin origin(ServerPlayer player) {
        return new Origin(player.level().dimension().identifier().toString(),
                player.getX(), player.getY(), player.getZ());
    }

    private static void loadKnownPlayers(Path playerDataDirectory) {
        KNOWN_PLAYERS.clear();
        if (!Files.isDirectory(playerDataDirectory)) return;
        try (var files = Files.list(playerDataDirectory)) {
            files.map(path -> path.getFileName().toString())
                    .filter(name -> PLAYER_DATA_FILE.matcher(name).matches())
                    .map(name -> name.substring(0, name.length() - 4))
                    .map(UUID::fromString)
                    .forEach(KNOWN_PLAYERS::add);
        } catch (IOException e) {
            Essentials.LOGGER.warn("Failed to inspect existing player data for first-join routing: {}",
                    e.toString());
        }
    }

    public enum Status {
        TELEPORTED, WARMING_UP, COOLDOWN, ALREADY_PENDING,
        NO_SPAWN, WORLD_UNAVAILABLE, UNSAFE
    }

    public record RequestResult(Status status, long seconds) {
    }

    private record DestinationResult(Status status, Optional<ServerLevel> world,
            Optional<Position> position) {
        static DestinationResult success(ServerLevel world, Position position) {
            return new DestinationResult(Status.TELEPORTED,
                    Optional.of(world), Optional.of(position));
        }

        static DestinationResult failure(Status status) {
            return new DestinationResult(status, Optional.empty(), Optional.empty());
        }
    }
}

package f1nal.essentials.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.DataResult;

import f1nal.essentials.Essentials;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.Util;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

/** Loads and safely saves the editable inventory data of an offline player. */
public final class OfflinePlayerDataManager {

    private static final int PLAYER_INVENTORY_SIZE = Inventory.SLOT_SADDLE + 1;
    private static final int ENDER_CHEST_SIZE = 27;

    private static final Map<UUID, Session> BY_TARGET = new HashMap<>();
    private static final Map<UUID, Session> BY_VIEWER = new HashMap<>();

    private OfflinePlayerDataManager() {
    }

    static AcquireResult acquire(MinecraftServer server, NameAndId target, UUID viewerId) {
        if (BY_TARGET.containsKey(target.id()) || BY_VIEWER.containsKey(viewerId)) {
            return new AcquireResult(AcquireStatus.BUSY, null);
        }

        var playerData = server.getPlayerList().loadPlayerData(target);
        if (playerData.isEmpty()) {
            return new AcquireResult(AcquireStatus.NOT_FOUND, null);
        }

        Session session = new Session(server, target, viewerId, playerData.get());
        BY_TARGET.put(target.id(), session);
        BY_VIEWER.put(viewerId, session);
        return new AcquireResult(AcquireStatus.READY, session);
    }

    public static void finishForViewer(UUID viewerId) {
        Session session = BY_VIEWER.get(viewerId);
        if (session != null) {
            session.finish();
        }
    }

    public static void finishAll() {
        for (Session session : List.copyOf(BY_TARGET.values())) {
            session.finish();
        }
    }

    enum AcquireStatus {
        READY,
        BUSY,
        NOT_FOUND
    }

    record AcquireResult(AcquireStatus status, Session session) {
    }

    static final class Session {

        private final MinecraftServer server;
        private final NameAndId target;
        private final UUID viewerId;
        private final net.minecraft.nbt.CompoundTag playerData;
        private final SimpleContainer inventory = new SimpleContainer(PLAYER_INVENTORY_SIZE);
        private final SimpleContainer enderChest = new SimpleContainer(ENDER_CHEST_SIZE);
        private boolean finished;

        private Session(MinecraftServer server, NameAndId target, UUID viewerId,
                net.minecraft.nbt.CompoundTag playerData) {
            this.server = server;
            this.target = target;
            this.viewerId = viewerId;
            this.playerData = playerData;
            loadContainer(inventory, playerData, "Inventory", server.registryAccess());
            loadContainer(enderChest, playerData, "EnderItems", server.registryAccess());
        }

        SimpleContainer inventory() {
            return inventory;
        }

        SimpleContainer enderChest() {
            return enderChest;
        }

        boolean isStillOffline() {
            return server.getPlayerList().getPlayer(target.id()) == null;
        }

        void finish() {
            if (finished) {
                return;
            }
            finished = true;
            BY_TARGET.remove(target.id(), this);
            BY_VIEWER.remove(viewerId, this);

            ServerPlayer onlineTarget = server.getPlayerList().getPlayer(target.id());
            if (onlineTarget != null) {
                copyIntoOnlinePlayer(onlineTarget);
                return;
            }

            saveContainer(inventory, playerData, "Inventory", server.registryAccess());
            saveContainer(enderChest, playerData, "EnderItems", server.registryAccess());
            savePlayerData();
        }

        private void copyIntoOnlinePlayer(ServerPlayer player) {
            for (int slot = 0; slot < Math.min(inventory.getContainerSize(), player.getInventory().getContainerSize()); slot++) {
                player.getInventory().setItem(slot, inventory.getItem(slot).copy());
            }
            for (int slot = 0; slot < enderChest.getContainerSize(); slot++) {
                player.getEnderChestInventory().setItem(slot, enderChest.getItem(slot).copy());
            }
            player.inventoryMenu.broadcastChanges();
            player.containerMenu.broadcastChanges();
        }

        private void savePlayerData() {
            Path directory = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
            Path current = directory.resolve(target.id() + ".dat");
            Path backup = directory.resolve(target.id() + ".dat_old");
            Path temporary = null;
            try {
                Files.createDirectories(directory);
                temporary = Files.createTempFile(directory, target.id() + "-", ".dat");
                NbtIo.writeCompressed(playerData, temporary);
                Util.safeReplaceFile(current, temporary, backup);
            } catch (Exception e) {
                Essentials.LOGGER.error("Failed to save offline inventory data for {}", target.name(), e);
            } finally {
                if (temporary != null) {
                    try {
                        Files.deleteIfExists(temporary);
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    private static void loadContainer(SimpleContainer container, net.minecraft.nbt.CompoundTag data,
            String key, RegistryAccess registries) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        Tag encoded = data.get(key);
        if (encoded == null) {
            encoded = new ListTag();
        }
        DataResult<List<ItemStackWithSlot>> decoded = ItemStackWithSlot.CODEC.listOf().parse(ops, encoded);
        decoded.error().ifPresent(error -> Essentials.LOGGER.warn(
                "Failed to decode offline player {}: {}", key, error.message()));
        for (ItemStackWithSlot entry : decoded.result().orElse(List.of())) {
            if (entry.isValidInContainer(container.getContainerSize())) {
                container.setItem(entry.slot(), entry.stack());
            }
        }
    }

    private static void saveContainer(SimpleContainer container, net.minecraft.nbt.CompoundTag data,
            String key, RegistryAccess registries) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        List<ItemStackWithSlot> items = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                items.add(new ItemStackWithSlot(slot, stack));
            }
        }
        DataResult<Tag> encoded = ItemStackWithSlot.CODEC.listOf().encodeStart(ops, items);
        encoded.error().ifPresent(error -> Essentials.LOGGER.warn(
                "Failed to encode offline player {}: {}", key, error.message()));
        encoded.result().ifPresent(tag -> data.put(key, tag));
    }
}

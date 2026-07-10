package f1nal.essentials.backpack;

import f1nal.essentials.Essentials;
import f1nal.essentials.config.BackpackConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import com.mojang.serialization.DataResult;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages backpack inventories with persistent storage. Supports both
 * per-player and serverwide modes.
 */
public final class BackpackManager {

    private static final Map<UUID, SimpleContainer> playerBackpacks = new ConcurrentHashMap<>();
    private static SimpleContainer serverwideBackpack = null;
    private static Path dataDir;

    private BackpackManager() {
    }

    public static void initialize(MinecraftServer server) {
        dataDir = FabricLoader.getInstance().getGameDir().resolve("essentials_data").resolve("backpacks");
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            Essentials.LOGGER.error("Failed to create backpacks data directory", e);
        }

        // Load serverwide backpack if in serverwide mode
        if (!BackpackConfig.get().perPlayer) {
            serverwideBackpack = loadServerwideBackpack(server);
        }
    }

    /**
     * Gets or creates a backpack inventory for the given player. In serverwide
     * mode, returns the shared backpack.
     */
    public static SimpleContainer getOrCreateBackpack(UUID playerId, MinecraftServer server) {
        if (!BackpackConfig.get().perPlayer) {
            // Serverwide mode - everyone shares the same backpack
            if (serverwideBackpack == null) {
                serverwideBackpack = loadServerwideBackpack(server);
            }
            return serverwideBackpack;
        }

        // Per-player mode
        return playerBackpacks.computeIfAbsent(playerId, id -> loadPlayerBackpack(id, server));
    }

    /**
     * Saves a player's backpack to disk. In serverwide mode, saves the shared
     * backpack.
     */
    public static void saveBackpack(UUID playerId, Container inventory, MinecraftServer server) {
        if (!BackpackConfig.get().perPlayer) {
            // Save serverwide backpack
            saveServerwideBackpack(inventory, server);
        } else {
            // Save per-player backpack
            savePlayerBackpack(playerId, inventory, server);
        }
    }

    /**
     * Saves all backpacks to disk (called on server shutdown).
     */
    public static void saveAll(MinecraftServer server) {
        if (!BackpackConfig.get().perPlayer) {
            if (serverwideBackpack != null) {
                saveServerwideBackpack(serverwideBackpack, server);
            }
        } else {
            for (Map.Entry<UUID, SimpleContainer> entry : playerBackpacks.entrySet()) {
                savePlayerBackpack(entry.getKey(), entry.getValue(), server);
            }
        }
    }

    /**
     * Saves a player's backpack to disk and unloads it from memory. Called on
     * disconnect: vanilla does not close open menus when a player disconnects,
     * so edits made while the backpack was open would otherwise only exist in
     * the cached container and be lost by evicting it.
     */
    public static void saveAndUnloadPlayer(UUID playerId, MinecraftServer server) {
        if (BackpackConfig.get().perPlayer) {
            SimpleContainer inventory = playerBackpacks.remove(playerId);
            if (inventory != null) {
                savePlayerBackpack(playerId, inventory, server);
            }
        } else if (serverwideBackpack != null) {
            saveServerwideBackpack(serverwideBackpack, server);
        }
    }

    private static SimpleContainer loadPlayerBackpack(UUID playerId, MinecraftServer server) {
        SimpleContainer inventory = new SimpleContainer(27);
        Path file = dataDir.resolve(playerId.toString() + ".nbt");

        if (!Files.exists(file)) {
            return inventory;
        }

        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            CompoundTag nbt = NbtIo.readCompressed(fis, NbtAccounter.unlimitedHeap());
            loadInventoryFromNbt(inventory, nbt, server);
        } catch (IOException e) {
            Essentials.LOGGER.error("Failed to load backpack for player {}", playerId, e);
        }

        return inventory;
    }

    private static SimpleContainer loadServerwideBackpack(MinecraftServer server) {
        SimpleContainer inventory = new SimpleContainer(27);
        Path file = dataDir.resolve("serverwide.nbt");

        if (!Files.exists(file)) {
            return inventory;
        }

        try (FileInputStream fis = new FileInputStream(file.toFile())) {
            CompoundTag nbt = NbtIo.readCompressed(fis, NbtAccounter.unlimitedHeap());
            loadInventoryFromNbt(inventory, nbt, server);
        } catch (IOException e) {
            Essentials.LOGGER.error("Failed to load serverwide backpack", e);
        }

        return inventory;
    }

    private static void savePlayerBackpack(UUID playerId, Container inventory, MinecraftServer server) {
        Path file = dataDir.resolve(playerId.toString() + ".nbt");
        CompoundTag nbt = saveInventoryToNbt(inventory, server);

        try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
            NbtIo.writeCompressed(nbt, fos);
        } catch (IOException e) {
            Essentials.LOGGER.error("Failed to save backpack for player {}", playerId, e);
        }
    }

    private static void saveServerwideBackpack(Container inventory, MinecraftServer server) {
        Path file = dataDir.resolve("serverwide.nbt");
        CompoundTag nbt = saveInventoryToNbt(inventory, server);

        try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
            NbtIo.writeCompressed(nbt, fos);
        } catch (IOException e) {
            Essentials.LOGGER.error("Failed to save serverwide backpack", e);
        }
    }

    private static void loadInventoryFromNbt(SimpleContainer inventory, CompoundTag nbt, MinecraftServer server) {
        HolderLookup.Provider registries = server.registryAccess();
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        ListTag list = nbt.getList("Items").orElseGet(ListTag::new);

        // Initialize with empty stacks
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            inventory.setItem(i, ItemStack.EMPTY);
        }

        for (int i = 0; i < list.size(); i++) {
            var maybeTag = list.getCompound(i);
            if (maybeTag.isEmpty()) continue;
            CompoundTag stackTag = maybeTag.get();
            int slot = (stackTag.getByte("Slot").orElse((byte) -1)) & 255;
            if (slot >= 0 && slot < inventory.getContainerSize()) {
                DataResult<ItemStack> parsed = ItemStack.CODEC.parse(ops, stackTag);
                if (parsed.result().isEmpty()) {
                    Essentials.LOGGER.warn("Failed to load backpack item in slot {}: {}",
                            slot, parsed.error().map(e -> e.message()).orElse("unknown error"));
                }
                ItemStack stack = parsed.result().orElse(ItemStack.EMPTY);
                inventory.setItem(slot, stack);
            }
        }
    }

    private static CompoundTag saveInventoryToNbt(Container inventory, MinecraftServer server) {
        CompoundTag nbt = new CompoundTag();
        HolderLookup.Provider registries = server.registryAccess();
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);

        ListTag list = new ListTag();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                DataResult<Tag> encoded = ItemStack.CODEC.encodeStart(ops, stack);
                if (encoded.result().isEmpty()) {
                    Essentials.LOGGER.warn("Failed to save backpack item in slot {} ({}): {}",
                            i, stack.getHoverName().getString(),
                            encoded.error().map(e -> e.message()).orElse("unknown error"));
                    continue;
                }
                if (encoded.result().get() instanceof CompoundTag stackTag) {
                    stackTag.putByte("Slot", (byte) i);
                    list.add(stackTag);
                }
            }
        }

        nbt.put("Items", list);
        return nbt;
    }
}

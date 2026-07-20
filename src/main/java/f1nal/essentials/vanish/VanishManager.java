package f1nal.essentials.vanish;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import f1nal.essentials.Essentials;
import f1nal.essentials.config.ConfigPaths;
import f1nal.essentials.config.CommandConfig.CommandSettings;
import f1nal.essentials.config.VanishConfig;
import f1nal.essentials.messaging.MessageFormatter;
import f1nal.essentials.messaging.VanishVisibility;
import f1nal.essentials.tpa.TpaManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Server lifecycle and viewer-specific visibility operations for vanish mode. */
public final class VanishManager {
    private static MinecraftServer server;
    private static VanishStore store;
    private static CommandSettings commandSettings = new CommandSettings(true, "op");

    private VanishManager() {
    }

    public static void configurePermissions(CommandSettings settings) {
        if (settings != null) commandSettings = settings;
    }

    public static void initialize(MinecraftServer minecraftServer) {
        server = minecraftServer;
        store = new VanishStore(ConfigPaths.vanishedPlayersFile());
        VanishVisibility.clear();
        if (!VanishConfig.get().persistState) return;
        try {
            VanishVisibility.replace(store.load());
        } catch (IOException e) {
            Essentials.LOGGER.warn("Failed to load vanished player state: {}", e.toString());
        }
    }

    public static void close() {
        if (VanishConfig.get().persistState) save();
        VanishVisibility.clear();
        server = null;
        store = null;
    }

    public static boolean isVanished(UUID playerId) {
        return VanishVisibility.isVanished(playerId);
    }

    public static boolean setVanished(ServerPlayer target, boolean vanished) {
        boolean changed = isVanished(target.getUUID()) != vanished;
        VanishVisibility.setVanished(target.getUUID(), vanished);
        if (vanished) TpaManager.removeAllFor(target.getUUID());
        if (changed && VanishConfig.get().persistState) save();
        refresh(target);
        return changed;
    }

    public static void onJoin(ServerPlayer player) {
        if (!VanishConfig.get().persistState) VanishVisibility.setVanished(player.getUUID(), false);
        refresh(player);
        if (VanishConfig.get().suppressAnnouncements && isVanished(player.getUUID())) broadcastStaffMessage(
                format(VanishConfig.get().joinMessage, player.getName().getString(), ""), player);
    }

    public static void onDisconnect(ServerPlayer player) {
        if (VanishConfig.get().suppressAnnouncements && isVanished(player.getUUID())) {
            broadcastStaffMessage(format(VanishConfig.get().leaveMessage,
                    player.getName().getString(), ""), player);
        }
        if (!VanishConfig.get().persistState) VanishVisibility.setVanished(player.getUUID(), false);
    }

    public static boolean canSee(CommandSourceStack viewer, ServerPlayer target) {
        if (!isVanished(target.getUUID()) || viewer.getPlayer() == target) return true;
        if (viewer.getPlayer() == null) return true;
        return commandSettings.getPermissionRequirement("vanish.see").test(viewer);
    }

    public static boolean canSee(ServerPlayer viewer, ServerPlayer target) {
        return canSee(viewer.createCommandSourceStack(), target);
    }

    public static void refresh(ServerPlayer target) {
        MinecraftServer current = server;
        if (current == null) return;
        for (ServerPlayer viewer : current.getPlayerList().getPlayers()) {
            if (canSee(viewer, target)) showTab(viewer, target);
            else hide(viewer, target);
        }
    }

    private static void showTab(ServerPlayer viewer, ServerPlayer target) {
        if (!VanishConfig.get().hideFromTabList) return;
        viewer.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(target)));
    }

    private static void hide(ServerPlayer viewer, ServerPlayer target) {
        if (VanishConfig.get().hideFromTabList) {
            viewer.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(target.getUUID())));
        }
        viewer.connection.send(new ClientboundRemoveEntitiesPacket(target.getId()));
    }

    public static void broadcastStaffMessage(Component message, ServerPlayer subject) {
        MinecraftServer current = server;
        if (current == null) return;
        for (ServerPlayer viewer : current.getPlayerList().getPlayers()) {
            if (canSee(viewer, subject)) viewer.sendSystemMessage(message);
        }
    }

    public static Component format(String template, String player, String message) {
        return MessageFormatter.format(template.replace("{player}", player), player, player, message);
    }

    private static void save() {
        if (store == null) return;
        try {
            store.save(VanishVisibility.snapshot());
        } catch (IOException e) {
            Essentials.LOGGER.error("Failed to save vanished player state", e);
        }
    }
}

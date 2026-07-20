package f1nal.essentials.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import f1nal.essentials.config.VanishConfig;
import f1nal.essentials.vanish.VanishManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;

/** Splits tab-list broadcasts so hidden entries never reach unauthorized viewers. */
@Mixin(PlayerList.class)
abstract class PlayerListMixin {
    @Shadow @Final private List<ServerPlayer> players;

    @Inject(method = "broadcastAll(Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"), cancellable = true)
    private void essentials$filterTabUpdates(Packet<?> packet, CallbackInfo callback) {
        if (!VanishConfig.get().hideFromTabList
                || !(packet instanceof ClientboundPlayerInfoUpdatePacket info)) return;
        boolean containsVanished = info.entries().stream()
                .anyMatch(entry -> VanishManager.isVanished(entry.profileId()));
        if (!containsVanished) return;

        for (ServerPlayer viewer : players) {
            List<ServerPlayer> visibleEntries = info.entries().stream()
                    .map(entry -> find(entry.profileId()))
                    .filter(target -> target != null && VanishManager.canSee(viewer, target))
                    .toList();
            if (!visibleEntries.isEmpty()) {
                viewer.connection.send(new ClientboundPlayerInfoUpdatePacket(
                        info.actions(), visibleEntries));
            }
        }
        callback.cancel();
    }

    private ServerPlayer find(java.util.UUID playerId) {
        for (ServerPlayer player : players) {
            if (player.getUUID().equals(playerId)) return player;
        }
        return null;
    }

    @Redirect(method = "placeNewPlayer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket;createPlayerInitializing(Ljava/util/Collection;)Lnet/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket;",
                    ordinal = 0))
    private ClientboundPlayerInfoUpdatePacket essentials$filterInitialTabList(
            java.util.Collection<ServerPlayer> entries, Connection connection,
            ServerPlayer joining, CommonListenerCookie cookie) {
        List<ServerPlayer> visible = entries.stream()
                .filter(target -> VanishManager.canSee(joining, target)).toList();
        return ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(visible);
    }

    @Redirect(method = "placeNewPlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void essentials$filterJoinAnnouncement(PlayerList list, Component message,
            boolean overlay, Connection connection, ServerPlayer player,
            CommonListenerCookie cookie) {
        if (VanishConfig.get().suppressAnnouncements
                && VanishManager.isVanished(player.getUUID())) return;
        list.broadcastSystemMessage(message, overlay);
    }

}

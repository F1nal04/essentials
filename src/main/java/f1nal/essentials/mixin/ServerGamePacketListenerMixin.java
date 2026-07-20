package f1nal.essentials.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import f1nal.essentials.config.VanishConfig;
import f1nal.essentials.vanish.VanishManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;

/** Suppresses the vanilla leave broadcast for vanished players. */
@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerMixin {
    @Shadow public ServerPlayer player;

    @Redirect(method = "removePlayerFromWorld",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void essentials$filterLeaveAnnouncement(PlayerList list,
            Component message, boolean overlay) {
        if (VanishConfig.get().suppressAnnouncements
                && VanishManager.isVanished(player.getUUID())) return;
        list.broadcastSystemMessage(message, overlay);
    }
}

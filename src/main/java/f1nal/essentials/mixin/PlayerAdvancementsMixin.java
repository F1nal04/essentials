package f1nal.essentials.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import f1nal.essentials.config.VanishConfig;
import f1nal.essentials.vanish.VanishManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

/** Restricts vanished-player advancement announcements to authorized viewers. */
@Mixin(PlayerAdvancements.class)
abstract class PlayerAdvancementsMixin {
    @Shadow private ServerPlayer player;

    @Redirect(method = "lambda$award$0",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void essentials$filterAdvancementAnnouncement(PlayerList list,
            Component message, boolean overlay) {
        if (VanishConfig.get().suppressAnnouncements
                && VanishManager.isVanished(player.getUUID())) {
            VanishManager.broadcastStaffMessage(message, player);
        } else {
            list.broadcastSystemMessage(message, overlay);
        }
    }
}

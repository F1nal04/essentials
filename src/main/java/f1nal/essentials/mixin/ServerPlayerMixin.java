package f1nal.essentials.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import f1nal.essentials.config.VanishConfig;
import f1nal.essentials.vanish.VanishManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;

/** Restricts death announcements for vanished players to authorized viewers. */
@Mixin(ServerPlayer.class)
abstract class ServerPlayerMixin {
    @Redirect(method = "die",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void essentials$filterDeathAnnouncement(PlayerList list, Component message,
            boolean overlay) {
        ServerPlayer subject = (ServerPlayer) (Object) this;
        if (VanishConfig.get().suppressAnnouncements
                && VanishManager.isVanished(subject.getUUID())) {
            VanishManager.broadcastStaffMessage(message, subject);
        } else {
            list.broadcastSystemMessage(message, overlay);
        }
    }

    @Redirect(method = "die",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemToTeam(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;)V"))
    private void essentials$filterTeamDeathAnnouncement(PlayerList list, Player player,
            Component message) {
        filterTeamDeath(list, player, message, true);
    }

    @Redirect(method = "die",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemToAllExceptTeam(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;)V"))
    private void essentials$filterOtherTeamDeathAnnouncement(PlayerList list, Player player,
            Component message) {
        filterTeamDeath(list, player, message, false);
    }

    private void filterTeamDeath(PlayerList list, Player player, Component message,
            boolean ownTeam) {
        ServerPlayer subject = (ServerPlayer) (Object) this;
        if (VanishConfig.get().suppressAnnouncements
                && VanishManager.isVanished(subject.getUUID())) {
            VanishManager.broadcastStaffMessage(message, subject);
        } else if (ownTeam) {
            list.broadcastSystemToTeam(player, message);
        } else {
            list.broadcastSystemToAllExceptTeam(player, message);
        }
    }
}

package f1nal.essentials.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import f1nal.essentials.moderation.MuteEnforcement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;

@Mixin(net.minecraft.server.commands.TeamMsgCommand.class)
abstract class TeamMsgCommandMixin {
    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private static void essentials$blockMutedTeamMessage(
            CommandSourceStack source,
            Entity sender,
            PlayerTeam team,
            List<ServerPlayer> targets,
            PlayerChatMessage message,
            CallbackInfo callback) {
        if (!MuteEnforcement.allowPrivateMessage(source)) {
            callback.cancel();
        }
    }
}

package f1nal.essentials.mixin;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import f1nal.essentials.moderation.MuteEnforcement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;

@Mixin(net.minecraft.server.commands.MsgCommand.class)
abstract class MsgCommandMixin {
    @Inject(method = "sendMessage", at = @At("HEAD"), cancellable = true)
    private static void essentials$blockMutedPrivateMessage(
            CommandSourceStack source,
            Collection<ServerPlayer> targets,
            PlayerChatMessage message,
            CallbackInfo callback) {
        if (!MuteEnforcement.allowPrivateMessage(source)) {
            callback.cancel();
        }
    }
}

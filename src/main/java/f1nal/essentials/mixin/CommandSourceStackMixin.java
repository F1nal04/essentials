package f1nal.essentials.mixin;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import f1nal.essentials.vanish.VanishManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/** Filters generic online-player command suggestions per requesting player. */
@Mixin(CommandSourceStack.class)
abstract class CommandSourceStackMixin {
    @Inject(method = "getOnlinePlayerNames", at = @At("RETURN"), cancellable = true)
    private void essentials$filterPlayerNames(CallbackInfoReturnable<Collection<String>> callback) {
        CommandSourceStack source = (CommandSourceStack) (Object) this;
        callback.setReturnValue(callback.getReturnValue().stream().filter(name -> {
            ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(name);
            return target == null || VanishManager.canSee(source, target);
        }).toList());
    }
}

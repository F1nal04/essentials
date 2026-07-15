package f1nal.essentials.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;

/** Prevents the vanilla command from bypassing mandatory kick auditing. */
@Mixin(net.minecraft.server.commands.KickCommand.class)
abstract class KickCommandMixin {

    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private static void essentials$replaceVanillaKick(
            CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo callback) {
        callback.cancel();
    }
}

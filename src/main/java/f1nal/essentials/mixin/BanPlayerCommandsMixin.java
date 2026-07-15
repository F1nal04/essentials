package f1nal.essentials.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.BanPlayerCommands;

/** Prevents the vanilla command from remaining as an unpersisted syntax path. */
@Mixin(BanPlayerCommands.class)
abstract class BanPlayerCommandsMixin {

    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private static void essentials$replaceVanillaBan(
            CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo callback) {
        callback.cancel();
    }
}

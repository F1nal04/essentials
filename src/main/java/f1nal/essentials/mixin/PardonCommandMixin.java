package f1nal.essentials.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.PardonCommand;

/** Prevents vanilla pardon from bypassing the persisted Essentials ban state. */
@Mixin(PardonCommand.class)
abstract class PardonCommandMixin {

    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private static void essentials$replaceVanillaPardon(
            CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo callback) {
        callback.cancel();
    }
}

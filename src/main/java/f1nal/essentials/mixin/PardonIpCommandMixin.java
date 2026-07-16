package f1nal.essentials.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.PardonIpCommand;

/** Prevents vanilla pardon-ip from bypassing the persisted Essentials IP-ban state. */
@Mixin(PardonIpCommand.class)
abstract class PardonIpCommandMixin {

    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private static void essentials$replaceVanillaPardonIp(
            CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo callback) {
        callback.cancel();
    }
}

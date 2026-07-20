package f1nal.essentials.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import f1nal.essentials.config.VanishConfig;
import f1nal.essentials.vanish.VanishManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Prevents collision between a hidden player and a player who cannot see them. */
@Mixin(Entity.class)
abstract class EntityMixin {
    @Inject(method = "canCollideWith", at = @At("HEAD"), cancellable = true)
    private void essentials$preventHiddenCollision(Entity other,
            CallbackInfoReturnable<Boolean> callback) {
        if (!VanishConfig.get().preventCollision) return;
        Entity self = (Entity) (Object) this;
        if (self instanceof ServerPlayer first && other instanceof ServerPlayer second
                && ((!VanishManager.canSee(second, first))
                || (!VanishManager.canSee(first, second)))) {
            callback.setReturnValue(false);
        }
    }
}

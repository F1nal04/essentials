package f1nal.essentials.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import f1nal.essentials.config.VanishConfig;
import f1nal.essentials.vanish.VanishManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Stops mobs from acquiring or retaining vanished players as targets. */
@Mixin(Mob.class)
abstract class MobMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void essentials$ignoreVanishedTarget(LivingEntity target, CallbackInfo callback) {
        if (VanishConfig.get().preventMobTargeting && target instanceof ServerPlayer player
                && VanishManager.isVanished(player.getUUID())) {
            ((Mob) (Object) this).setTarget(null);
            callback.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void essentials$dropVanishedTarget(CallbackInfo callback) {
        Mob mob = (Mob) (Object) this;
        if (VanishConfig.get().preventMobTargeting
                && mob.getTarget() instanceof ServerPlayer player
                && VanishManager.isVanished(player.getUUID())) {
            mob.setTarget(null);
        }
    }
}

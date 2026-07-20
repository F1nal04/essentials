package f1nal.essentials.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import f1nal.essentials.vanish.VanishManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Keeps hidden players out of each unauthorized viewer's entity tracker. */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
abstract class ChunkMapTrackedEntityMixin {
    @Shadow private Entity entity;
    @Shadow public abstract void removePlayer(ServerPlayer player);

    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void essentials$hideVanishedPlayer(ServerPlayer viewer, CallbackInfo callback) {
        if (entity instanceof ServerPlayer target && !VanishManager.canSee(viewer, target)) {
            removePlayer(viewer);
            callback.cancel();
        }
    }
}

package f1nal.essentials.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import f1nal.essentials.vanish.VanishManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Removes vanished players from selectors unless the source may see them. */
@Mixin(EntitySelector.class)
abstract class EntitySelectorMixin {
    @Inject(method = "findPlayers", at = @At("RETURN"), cancellable = true)
    private void essentials$filterPlayers(CommandSourceStack source,
            CallbackInfoReturnable<List<ServerPlayer>> callback) {
        callback.setReturnValue(callback.getReturnValue().stream()
                .filter(player -> VanishManager.canSee(source, player)).toList());
    }

    @Inject(method = "findEntities", at = @At("RETURN"), cancellable = true)
    private void essentials$filterEntities(CommandSourceStack source,
            CallbackInfoReturnable<List<? extends Entity>> callback) {
        List<Entity> filtered = new ArrayList<>();
        for (Entity entity : callback.getReturnValue()) {
            if (!(entity instanceof ServerPlayer player) || VanishManager.canSee(source, player)) {
                filtered.add(entity);
            }
        }
        callback.setReturnValue(filtered);
    }
}

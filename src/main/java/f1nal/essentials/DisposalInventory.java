package f1nal.essentials;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class DisposalInventory extends SimpleInventory {
    public DisposalInventory() {
        super(27);
    }

    @Override
    public void onClose(PlayerEntity player) {
        if (!player.getWorld().isClient) {
            clear();
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.sendMessage(Text.literal("The items in the disposal were deleted."), false);
            }
        }
        super.onClose(player);
    }
}

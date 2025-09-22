package f1nal.essentials;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class DisposalInventory extends SimpleInventory {

    public DisposalInventory() {
        super(27);
    }

    private int getTotalItemCount() {
        int total = 0;
        for (int i = 0; i < size(); i++) {
            ItemStack stack = getStack(i);
            if (!stack.isEmpty()) {
                total += stack.getCount();
            }
        }
        return total;
    }

    @Override
    public void onClose(PlayerEntity player) {
        if (!player.getWorld().isClient) {
            int deletedCount = getTotalItemCount();
            clear();
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.sendMessage(
                        Messages.info("Disposal closed. Deleted " + deletedCount + " item" + (deletedCount == 1 ? "" : "s") + "."),
                        false
                );
            }
        }
        super.onClose(player);
    }
}

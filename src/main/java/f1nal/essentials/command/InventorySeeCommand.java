package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class InventorySeeCommand {

    private InventorySeeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment, CommandConfig.CommandSettings settings) {
        LiteralArgumentBuilder<CommandSourceStack> isee = Commands.literal("isee")
                .requires(settings.getPermissionRequirement())
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> open(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))));

        dispatcher.register(isee);
    }

    private static int open(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer viewer = source.getPlayer();
        if (viewer == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }

        String name = target.getName().getString();
        viewer.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, player) -> new InventoryViewMenu(syncId, playerInventory, target),
                Component.literal(name + "'s Inventory")
        ));

        source.sendSuccess(() -> Messages.info("Viewing " + name + "'s inventory."), false);
        return 1;
    }

    /**
     * 9x5 chest-shaped menu over another player's live inventory, laid out
     * per {@link InventoryViewLayout}: main storage, hotbar, then armor,
     * offhand, and four locked fillers. Real slots edit the target's
     * inventory directly; vanilla live-syncs both sides every tick.
     */
    static final class InventoryViewMenu extends AbstractContainerMenu {

        private static final int VIEW_SLOTS = InventoryViewLayout.VIEW_SIZE;

        private final ServerPlayer target;

        InventoryViewMenu(int syncId, Inventory viewerInventory, ServerPlayer target) {
            super(MenuType.GENERIC_9x5, syncId);
            this.target = target;

            Inventory targetInventory = target.getInventory();
            Container fillers = new SimpleContainer(4);

            for (int view = 0; view < VIEW_SLOTS; view++) {
                int x = 8 + (view % 9) * 18;
                int y = 18 + (view / 9) * 18;
                int invIndex = InventoryViewLayout.mapSlot(view);
                if (invIndex == InventoryViewLayout.LOCKED) {
                    addSlot(new LockedSlot(fillers, view - 41, x, y));
                } else {
                    addSlot(new Slot(targetInventory, invIndex, x, y));
                }
            }

            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(viewerInventory, col + row * 9 + 9, 8 + col * 18, 121 + row * 18));
                }
            }
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(viewerInventory, col, 8 + col * 18, 179));
            }
        }

        @Override
        public boolean stillValid(Player player) {
            // Auto-close when the target logs out; the default check would
            // reject a viewer who doesn't own the inventory.
            return !target.hasDisconnected() && !target.isRemoved();
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            ItemStack result = ItemStack.EMPTY;
            Slot slot = this.slots.get(index);
            if (slot != null && slot.hasItem()) {
                ItemStack stack = slot.getItem();
                result = stack.copy();
                if (index < VIEW_SLOTS) {
                    if (!moveItemStackTo(stack, VIEW_SLOTS, this.slots.size(), true)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(stack, 0, VIEW_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
                if (stack.isEmpty()) {
                    slot.setByPlayer(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
            }
            return result;
        }

        private static final class LockedSlot extends Slot {

            LockedSlot(Container container, int index, int x, int y) {
                super(container, index, x, y);
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        }
    }
}

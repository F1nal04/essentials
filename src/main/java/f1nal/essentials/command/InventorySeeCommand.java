package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class InventorySeeCommand {

    private InventorySeeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment, CommandConfig.CommandSettings settings) {
        LiteralCommandNode<CommandSourceStack> inventorySee = dispatcher.register(
                command("inventorysee").requires(settings.getPermissionRequirement("inventorysee")));

        dispatcher.register(Commands.literal("isee")
                .requires(settings.getPermissionRequirement("isee"))
                .redirect(inventorySee));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> command(String name) {
        return Commands.literal(name)
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(ctx -> open(ctx.getSource(),
                                GameProfileArgument.getGameProfiles(ctx, "player"))));
    }

    private static int open(CommandSourceStack source,
            java.util.Collection<net.minecraft.server.players.NameAndId> targets) {
        ServerPlayer viewer = source.getPlayer();
        if (viewer == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }

        if (targets.size() != 1) {
            source.sendFailure(Messages.error("Please specify exactly one player."));
            return 0;
        }

        net.minecraft.server.players.NameAndId profile = targets.iterator().next();
        ServerPlayer target = source.getServer().getPlayerList().getPlayer(profile.id());
        if (target != null) {
            return openOnline(source, viewer, target);
        }

        OfflinePlayerDataManager.AcquireResult result = OfflinePlayerDataManager.acquire(
                source.getServer(), profile, viewer.getUUID());
        if (result.status() == OfflinePlayerDataManager.AcquireStatus.NOT_FOUND) {
            source.sendFailure(Messages.error(profile.name() + " has never joined this server."));
            return 0;
        }
        if (result.status() == OfflinePlayerDataManager.AcquireStatus.BUSY) {
            source.sendFailure(Messages.error("That player inventory is already being edited."));
            return 0;
        }

        OfflinePlayerDataManager.Session session = result.session();
        viewer.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, player) -> new InventoryViewMenu(syncId, playerInventory,
                        session.inventory(), null, session::isStillOffline, session::finish),
                Component.literal(profile.name() + "'s Inventory")
        ));
        source.sendSuccess(() -> Messages.info("Viewing " + profile.name() + "'s inventory."), false);
        return 1;
    }

    private static int openOnline(CommandSourceStack source, ServerPlayer viewer, ServerPlayer target) {
        String name = target.getName().getString();
        viewer.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, player) -> new InventoryViewMenu(syncId, playerInventory,
                        target.getInventory(), target,
                        () -> !target.hasDisconnected() && !target.isRemoved(), () -> { }),
                Component.literal(name + "'s Inventory")
        ));

        source.sendSuccess(() -> Messages.info("Viewing " + name + "'s inventory."), false);
        return 1;
    }

    /**
     * 9x6 chest-shaped menu over another player's inventory, laid out per
     * {@link InventoryViewLayout}: main storage, border, hotbar, then armor
     * and offhand. Real slots edit the target's inventory directly; vanilla
     * live-syncs both sides every tick for online targets.
     */
    static final class InventoryViewMenu extends AbstractContainerMenu {

        private static final int VIEW_SLOTS = InventoryViewLayout.VIEW_SIZE;

        private final java.util.function.BooleanSupplier validity;
        private final Runnable onClose;

        InventoryViewMenu(int syncId, Inventory viewerInventory, Container targetInventory,
                ServerPlayer target, java.util.function.BooleanSupplier validity, Runnable onClose) {
            super(MenuType.GENERIC_9x6, syncId);
            this.validity = validity;
            this.onClose = onClose;

            SimpleContainer fillers = new SimpleContainer(VIEW_SLOTS);

            for (int view = 0; view < VIEW_SLOTS; view++) {
                int x = 8 + (view % 9) * 18;
                int y = 18 + (view / 9) * 18;
                int invIndex = InventoryViewLayout.mapSlot(view);
                if (invIndex == InventoryViewLayout.LOCKED) {
                    fillers.setItem(view, borderItem(view));
                    addSlot(new LockedSlot(fillers, view, x, y));
                } else if (view >= 45 && view < 49) {
                    EquipmentSlot equipmentSlot = switch (invIndex) {
                        case 39 -> EquipmentSlot.HEAD;
                        case 38 -> EquipmentSlot.CHEST;
                        case 37 -> EquipmentSlot.LEGS;
                        case 36 -> EquipmentSlot.FEET;
                        default -> throw new IllegalStateException("Unexpected armor slot " + invIndex);
                    };
                    addSlot(new EquipmentViewSlot(targetInventory, invIndex, x, y,
                            target, equipmentSlot));
                } else {
                    addSlot(new Slot(targetInventory, invIndex, x, y));
                }
            }

            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(viewerInventory, col + row * 9 + 9, 8 + col * 18, 139 + row * 18));
                }
            }
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(viewerInventory, col, 8 + col * 18, 197));
            }
        }

        private static ItemStack borderItem(int view) {
            ItemStack border = new ItemStack(Items.STAINED_GLASS_PANE.pick(
                    view < 36 ? DyeColor.BLACK : DyeColor.GRAY));
            String label = view < 36
                    ? "Main Inventory ↑  •  Hotbar ↓"
                    : "← Armor  •  Off-Hand →";
            border.set(DataComponents.CUSTOM_NAME,
                    Component.literal(label).withStyle(ChatFormatting.GRAY));
            return border;
        }

        @Override
        public boolean stillValid(Player player) {
            return validity.getAsBoolean();
        }

        @Override
        public void removed(Player player) {
            super.removed(player);
            onClose.run();
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

            @Override
            public ItemStack safeClone(Player player) {
                return ItemStack.EMPTY;
            }
        }

        private static final class EquipmentViewSlot extends Slot {

            private final ServerPlayer target;
            private final EquipmentSlot equipmentSlot;

            EquipmentViewSlot(Container container, int index, int x, int y,
                    ServerPlayer target, EquipmentSlot equipmentSlot) {
                super(container, index, x, y);
                this.target = target;
                this.equipmentSlot = equipmentSlot;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                if (target != null) {
                    return target.isEquippableInSlot(stack, equipmentSlot);
                }
                net.minecraft.world.item.equipment.Equippable equippable =
                        stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
                return equippable != null && equippable.slot() == equipmentSlot;
            }
        }
    }
}

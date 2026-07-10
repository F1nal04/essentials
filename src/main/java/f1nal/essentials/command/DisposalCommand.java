package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

public final class DisposalCommand {

    private DisposalCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment, CommandConfig.CommandSettings settings) {
        LiteralArgumentBuilder<CommandSourceStack> disposal = Commands.literal("disposal")
                .requires(settings.getPermissionRequirement())
                .executes(ctx -> openDisposal(ctx.getSource(), ctx.getSource().getPlayer()));
        LiteralArgumentBuilder<CommandSourceStack> trash = Commands.literal("trash")
                .requires(settings.getPermissionRequirement())
                .executes(ctx -> openDisposal(ctx.getSource(), ctx.getSource().getPlayer()));
        LiteralArgumentBuilder<CommandSourceStack> trashcan = Commands.literal("trashcan")
                .requires(settings.getPermissionRequirement())
                .executes(ctx -> openDisposal(ctx.getSource(), ctx.getSource().getPlayer()));

        dispatcher.register(disposal);
        dispatcher.register(trash);
        dispatcher.register(trashcan);
    }

    private static int openDisposal(CommandSourceStack source, ServerPlayer target) {
        if (target == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }

        target.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, player) -> {
                    SimpleContainer inv = new SimpleContainer(27);
                    return new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x3, syncId, playerInventory, inv, 3) {
                        private int getTotalItemCount() {
                            int total = 0;
                            for (int i = 0; i < inv.getContainerSize(); i++) {
                                ItemStack stack = inv.getItem(i);
                                if (!stack.isEmpty()) {
                                    total += stack.getCount();
                                }
                            }
                            return total;
                        }
                        @Override
                        public void removed(Player playerEntity) {
                            if (!playerEntity.level().isClientSide()) {
                                int deletedCount = getTotalItemCount();
                                inv.clearContent();
                                if (playerEntity instanceof ServerPlayer serverPlayer) {
                                    serverPlayer.displayClientMessage(
                                            Messages.info("Disposal closed. Deleted " + deletedCount + " item" + (deletedCount == 1 ? "" : "s") + "."),
                                            false
                                    );
                                }
                            }
                            super.removed(playerEntity);
                        }
                    };
                },
                Component.literal("Disposal")
        ));

        if (source.getEntity() == target) {
            source.sendSuccess(() -> Messages.info("Opened disposal."), false);
        }

        return 1;
    }
}



package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;

public final class DisposalCommand {

    private DisposalCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment, CommandConfig.CommandSettings settings) {
        LiteralArgumentBuilder<ServerCommandSource> disposal = CommandManager.literal("disposal")
                .requires(settings.getPermissionRequirement())
                .executes(ctx -> openDisposal(ctx.getSource(), ctx.getSource().getPlayer()));
        LiteralArgumentBuilder<ServerCommandSource> trash = CommandManager.literal("trash")
                .requires(settings.getPermissionRequirement())
                .executes(ctx -> openDisposal(ctx.getSource(), ctx.getSource().getPlayer()));
        LiteralArgumentBuilder<ServerCommandSource> trashcan = CommandManager.literal("trashcan")
                .requires(settings.getPermissionRequirement())
                .executes(ctx -> openDisposal(ctx.getSource(), ctx.getSource().getPlayer()));

        dispatcher.register(disposal);
        dispatcher.register(trash);
        dispatcher.register(trashcan);
    }

    private static int openDisposal(ServerCommandSource source, ServerPlayerEntity target) {
        if (target == null) {
            source.sendError(Messages.error("You must be a player to use this command."));
            return 0;
        }

        target.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, player) -> {
                    SimpleInventory inv = new SimpleInventory(27);
                    return new GenericContainerScreenHandler(net.minecraft.screen.ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, inv, 3) {
                        private int getTotalItemCount() {
                            int total = 0;
                            for (int i = 0; i < inv.size(); i++) {
                                ItemStack stack = inv.getStack(i);
                                if (!stack.isEmpty()) {
                                    total += stack.getCount();
                                }
                            }
                            return total;
                        }
                        @Override
                        public void onClosed(PlayerEntity playerEntity) {
                            if (!playerEntity.getEntityWorld().isClient()) {
                                int deletedCount = getTotalItemCount();
                                inv.clear();
                                if (playerEntity instanceof ServerPlayerEntity serverPlayer) {
                                    serverPlayer.sendMessage(
                                            Messages.info("Disposal closed. Deleted " + deletedCount + " item" + (deletedCount == 1 ? "" : "s") + "."),
                                            false
                                    );
                                }
                            }
                            super.onClosed(playerEntity);
                        }
                    };
                },
                Text.literal("Disposal")
        ));

        if (source.getEntity() == target) {
            source.sendFeedback(() -> Messages.info("Opened disposal."), false);
        }

        return 1;
    }
}



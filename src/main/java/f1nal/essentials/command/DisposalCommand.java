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

public final class DisposalCommand {

    private DisposalCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> disposal = CommandManager.literal("disposal")
                .executes(ctx -> openDisposal(ctx.getSource(), ctx.getSource().getPlayer()));
        LiteralArgumentBuilder<ServerCommandSource> trash = CommandManager.literal("trash")
                .executes(ctx -> openDisposal(ctx.getSource(), ctx.getSource().getPlayer()));
        LiteralArgumentBuilder<ServerCommandSource> trashcan = CommandManager.literal("trashcan")
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
                (syncId, playerInventory, player) -> GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, new SimpleInventory(27) {
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
                    public void onClose(PlayerEntity playerEntity) {
                        if (!playerEntity.getWorld().isClient) {
                            int deletedCount = getTotalItemCount();
                            clear();
                            if (playerEntity instanceof ServerPlayerEntity serverPlayer) {
                                serverPlayer.sendMessage(
                                        Messages.info("Disposal closed. Deleted " + deletedCount + " item" + (deletedCount == 1 ? "" : "s") + "."),
                                        false
                                );
                            }
                        }
                        super.onClose(playerEntity);
                    }
                }),
                Text.literal("Disposal")
        ));

        if (source.getEntity() == target) {
            source.sendFeedback(() -> Messages.info("Opened disposal."), false);
        }

        return 1;
    }
}



package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import f1nal.essentials.Messages;
import f1nal.essentials.backpack.BackpackManager;
import f1nal.essentials.config.BackpackConfig;
import f1nal.essentials.config.CommandConfig;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class BackpackCommand {

    private BackpackCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment, CommandConfig.CommandSettings settings) {
        LiteralArgumentBuilder<ServerCommandSource> backpack = CommandManager.literal("backpack")
                .requires(settings.getPermissionRequirement())
                .executes(ctx -> openBackpack(ctx.getSource(), ctx.getSource().getPlayer()));

        LiteralArgumentBuilder<ServerCommandSource> bp = CommandManager.literal("bp")
                .requires(settings.getPermissionRequirement())
                .executes(ctx -> openBackpack(ctx.getSource(), ctx.getSource().getPlayer()));

        dispatcher.register(backpack);
        dispatcher.register(bp);
    }

    private static int openBackpack(ServerCommandSource source, ServerPlayerEntity target) {
        if (target == null) {
            source.sendError(Messages.error("You must be a player to use this command."));
            return 0;
        }

        boolean perPlayer = BackpackConfig.get().perPlayer;
        String title = perPlayer ? "Backpack" : "Serverwide Backpack";

        target.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, player) -> {
                    SimpleInventory backpackInv = BackpackManager.getOrCreateBackpack(
                            player.getUuid(),
                            source.getServer()
                    );

                    return new GenericContainerScreenHandler(
                            net.minecraft.screen.ScreenHandlerType.GENERIC_9X3,
                            syncId,
                            playerInventory,
                            backpackInv,
                            3
                    ) {
                        @Override
                        public void onClosed(PlayerEntity playerEntity) {
                            if (playerEntity instanceof ServerPlayerEntity serverPlayer) {
                                // Save the backpack when closed (server side)
                                BackpackManager.saveBackpack(
                                        serverPlayer.getUuid(),
                                        backpackInv,
                                        source.getServer()
                                );

                                serverPlayer.sendMessage(
                                        Messages.info("Backpack closed."),
                                        false
                                );
                            }
                            super.onClosed(playerEntity);
                        }
            };
                },
                Text.literal(title)
        ));

        if (source.getEntity() == target) {
            source.sendFeedback(() -> Messages.info("Opened " + (perPlayer ? "your" : "the serverwide") + " backpack."), false);
        }

        return 1;
    }
}

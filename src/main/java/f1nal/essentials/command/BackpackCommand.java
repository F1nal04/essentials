package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import f1nal.essentials.Messages;
import f1nal.essentials.backpack.BackpackManager;
import f1nal.essentials.config.BackpackConfig;
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

public final class BackpackCommand {

    private BackpackCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment, CommandConfig.CommandSettings settings) {
        LiteralArgumentBuilder<CommandSourceStack> backpack = Commands.literal("backpack")
                .requires(settings.getPermissionRequirement())
                .executes(ctx -> openBackpack(ctx.getSource(), ctx.getSource().getPlayer()));

        LiteralArgumentBuilder<CommandSourceStack> bp = Commands.literal("bp")
                .requires(settings.getPermissionRequirement())
                .executes(ctx -> openBackpack(ctx.getSource(), ctx.getSource().getPlayer()));

        dispatcher.register(backpack);
        dispatcher.register(bp);
    }

    private static int openBackpack(CommandSourceStack source, ServerPlayer target) {
        if (target == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }

        boolean perPlayer = BackpackConfig.get().perPlayer;
        String title = perPlayer ? "Backpack" : "Serverwide Backpack";

        target.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, player) -> {
                    SimpleContainer backpackInv = BackpackManager.getOrCreateBackpack(
                            player.getUUID(),
                            source.getServer()
                    );

                    return new ChestMenu(
                            net.minecraft.world.inventory.MenuType.GENERIC_9x3,
                            syncId,
                            playerInventory,
                            backpackInv,
                            3
                    ) {
                        @Override
                        public void removed(Player playerEntity) {
                            if (playerEntity instanceof ServerPlayer serverPlayer) {
                                // Save the backpack when closed (server side)
                                BackpackManager.saveBackpack(
                                        serverPlayer.getUUID(),
                                        backpackInv,
                                        source.getServer()
                                );

                                serverPlayer.displayClientMessage(
                                        Messages.info("Backpack closed."),
                                        false
                                );
                            }
                            super.removed(playerEntity);
                        }
            };
                },
                Component.literal(title)
        ));

        if (source.getEntity() == target) {
            source.sendSuccess(() -> Messages.info("Opened " + (perPlayer ? "your" : "the serverwide") + " backpack."), false);
        }

        return 1;
    }
}

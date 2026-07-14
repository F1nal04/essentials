package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
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
        LiteralCommandNode<CommandSourceStack> backpack = dispatcher.register(Commands.literal("backpack")
                .requires(settings.getPermissionRequirement())
                .executes(ctx -> openBackpack(ctx.getSource(), ctx.getSource().getPlayer())));

        dispatcher.register(Commands.literal("bp")
                .requires(settings.getPermissionRequirement())
                .executes(backpack.getCommand())
                .redirect(backpack));
    }

    private static int openBackpack(CommandSourceStack source, ServerPlayer target) {
        if (target == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }

        BackpackConfig.Mode mode = BackpackConfig.get().mode;

        if (mode == BackpackConfig.Mode.ENDER_CHEST) {
            // The backpack is the player's vanilla ender chest: same live
            // container, so vanilla owns persistence and no save is needed.
            target.openMenu(new SimpleMenuProvider(
                    (syncId, playerInventory, player) -> ChestMenu.threeRows(syncId, playerInventory, player.getEnderChestInventory()),
                    Component.literal("Ender Chest")
            ));

            if (source.getEntity() == target) {
                source.sendSuccess(() -> Messages.info("Opened your ender chest."), false);
            }

            return 1;
        }

        boolean perPlayer = mode == BackpackConfig.Mode.PER_PLAYER;
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

                                serverPlayer.sendSystemMessage(
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

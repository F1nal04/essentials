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
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;

public final class EnderChestSeeCommand {

    private EnderChestSeeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment, CommandConfig.CommandSettings settings) {
        LiteralArgumentBuilder<CommandSourceStack> esee = Commands.literal("esee")
                .requires(settings.getPermissionRequirement())
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> open(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"))));

        dispatcher.register(esee);
    }

    private static int open(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer viewer = source.getPlayer();
        if (viewer == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }

        String name = target.getName().getString();
        viewer.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, player) -> ChestMenu.threeRows(syncId, playerInventory, target.getEnderChestInventory()),
                Component.literal(name + "'s Ender Chest")
        ));

        source.sendSuccess(() -> Messages.info("Viewing " + name + "'s ender chest."), false);
        return 1;
    }
}

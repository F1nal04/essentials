package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public final class FeedCommand {

    private FeedCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment, CommandConfig.CommandSettings settings) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("feed")
                .requires(settings.getPermissionRequirement())
                .executes(ctx -> feed(ctx.getSource(), ctx.getSource().getPlayer()))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> feed(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"))));

        dispatcher.register(root);
    }

    private static int feed(CommandSourceStack source, ServerPlayer target) {
        if (target == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }

        target.getFoodData().setFoodLevel(20);
        target.getFoodData().setSaturation(20.0F);

        if (source.getEntity() == target) {
            source.sendSuccess(() -> Messages.info("Fed to full hunger and saturation."), false);
        } else {
            source.sendSuccess(() -> Messages.info("Fed " + target.getName().getString() + " to full hunger and saturation."), true);
            target.sendSystemMessage(Messages.info("Fed to full hunger and saturation by " + source.getTextName() + "."));
        }

        return 1;
    }
}

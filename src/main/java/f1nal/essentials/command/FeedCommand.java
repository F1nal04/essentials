package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import f1nal.essentials.Messages;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class FeedCommand {

    private FeedCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("feed")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(ctx -> feed(ctx.getSource(), ctx.getSource().getPlayer()))
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .executes(ctx -> feed(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "target"))));

        dispatcher.register(root);
    }

    private static int feed(ServerCommandSource source, ServerPlayerEntity target) {
        if (target == null) {
            source.sendError(Messages.error("You must be a player to use this command."));
            return 0;
        }

        target.getHungerManager().setFoodLevel(20);
        target.getHungerManager().setSaturationLevel(20.0F);

        if (source.getEntity() == target) {
            source.sendFeedback(() -> Messages.info("Fed to full hunger and saturation."), false);
        } else {
            source.sendFeedback(() -> Messages.info("Fed " + target.getName().getString() + " to full hunger and saturation."), true);
            target.sendMessage(Messages.info("Fed to full hunger and saturation by " + source.getName() + "."));
        }

        return 1;
    }
}

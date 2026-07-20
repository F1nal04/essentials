package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig.CommandSettings;
import f1nal.essentials.config.VanishConfig;
import f1nal.essentials.vanish.VanishManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/** /vanish toggles the executor or an explicitly targeted online player. */
public final class VanishCommand {
    private VanishCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess, Commands.CommandSelection environment,
            CommandSettings settings) {
        dispatcher.register(Commands.literal("vanish")
                .requires(settings.getPermissionRequirement("vanish"))
                .executes(context -> set(context, context.getSource().getPlayer(), null))
                .then(Commands.literal("on")
                        .executes(context -> set(context, context.getSource().getPlayer(), true)))
                .then(Commands.literal("off")
                        .executes(context -> set(context, context.getSource().getPlayer(), false)))
                .then(Commands.argument("target", EntityArgument.player())
                        .requires(settings.getPermissionRequirement("vanish.others"))
                        .executes(context -> set(context,
                                EntityArgument.getPlayer(context, "target"), null))
                        .then(Commands.literal("on").executes(context -> set(context,
                                EntityArgument.getPlayer(context, "target"), true)))
                        .then(Commands.literal("off").executes(context -> set(context,
                                EntityArgument.getPlayer(context, "target"), false)))));
    }

    private static int set(CommandContext<CommandSourceStack> context,
            ServerPlayer target, Boolean requested) {
        CommandSourceStack source = context.getSource();
        if (target == null) {
            source.sendFailure(Messages.error("You must be a player or specify a target."));
            return 0;
        }
        boolean vanished = requested != null ? requested : !VanishManager.isVanished(target.getUUID());
        VanishManager.setVanished(target, vanished);
        String template = vanished ? VanishConfig.get().enabledMessage : VanishConfig.get().disabledMessage;
        source.sendSuccess(() -> VanishManager.format(template,
                target.getName().getString(), ""), false);
        if (source.getPlayer() != target) {
            target.sendSystemMessage(VanishManager.format(template,
                    target.getName().getString(), ""));
        }
        return 1;
    }
}

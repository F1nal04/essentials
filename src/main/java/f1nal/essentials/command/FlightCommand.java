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

public final class FlightCommand {

    private FlightCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment, CommandConfig.CommandSettings settings) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("flight")
                .requires(settings.getPermissionRequirement())
                .executes(ctx -> toggleFlight(ctx.getSource(), ctx.getSource().getPlayer()))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> toggleFlight(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"))));

        dispatcher.register(root);
    }

    private static int toggleFlight(CommandSourceStack source, ServerPlayer target) {
        if (target == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }

        if (target.isCreative() || target.isSpectator()) {
            if (source.getEntity() == target) {
                source.sendSuccess(() -> Messages.info("Gamemode already allows flight."), false);
            } else {
                source.sendSuccess(() -> Messages.info(target.getName().getString() + "'s gamemode already allows flight."), true);
            }
            return 1;
        }

        boolean enable = !target.getAbilities().mayfly;
        target.getAbilities().mayfly = enable;
        target.getAbilities().flying = enable;
        target.onUpdateAbilities();

        if (source.getEntity() == target) {
            source.sendSuccess(() -> (enable ? Messages.success("Flight enabled.") : Messages.info("Flight disabled.")), false);
        } else {
            source.sendSuccess(() -> (enable
                    ? Messages.success("Enabled " + target.getName().getString() + "'s flight.")
                    : Messages.warning("Disabled " + target.getName().getString() + "'s flight.")), true);
            target.sendSystemMessage(Messages.info("Your flight was " + (enable ? "enabled" : "disabled") + " by " + source.getTextName() + "."));
        }

        return 1;
    }
}

package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import f1nal.essentials.Messages;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class FlightCommand {

    private FlightCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("flight")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(ctx -> toggleFlight(ctx.getSource(), ctx.getSource().getPlayer()))
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .executes(ctx -> toggleFlight(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "target"))));

        dispatcher.register(root);
    }

    private static int toggleFlight(ServerCommandSource source, ServerPlayerEntity target) {
        if (target == null) {
            source.sendError(Messages.error("You must be a player to use this command."));
            return 0;
        }

        if (target.isCreative() || target.isSpectator()) {
            if (source.getEntity() == target) {
                source.sendFeedback(() -> Messages.info("Gamemode already allows flight."), false);
            } else {
                source.sendFeedback(() -> Messages.info(target.getName().getString() + "'s gamemode already allows flight."), true);
            }
            return 1;
        }

        boolean enable = !target.getAbilities().allowFlying;
        target.getAbilities().allowFlying = enable;
        target.getAbilities().flying = enable;
        target.sendAbilitiesUpdate();

        if (source.getEntity() == target) {
            source.sendFeedback(() -> (enable ? Messages.success("Flight enabled.") : Messages.info("Flight disabled.")), false);
        } else {
            source.sendFeedback(() -> (enable
                    ? Messages.success("Enabled " + target.getName().getString() + "'s flight.")
                    : Messages.warning("Disabled " + target.getName().getString() + "'s flight.")), true);
            target.sendMessage(Messages.info("Your flight was " + (enable ? "enabled" : "disabled") + " by " + source.getName() + "."));
        }

        return 1;
    }
}

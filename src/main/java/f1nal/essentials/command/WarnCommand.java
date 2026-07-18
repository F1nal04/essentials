package f1nal.essentials.command;

import java.sql.SQLException;
import java.util.Collection;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import f1nal.essentials.Essentials;
import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.config.ModerationConfig;
import f1nal.essentials.moderation.ModerationManager;
import f1nal.essentials.moderation.ModerationMessages;
import f1nal.essentials.moderation.WarningResult;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public final class WarnCommand {
    private WarnCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess, Commands.CommandSelection environment,
            CommandConfig.CommandSettings settings) {
        dispatcher.register(Commands.literal("warn")
                .requires(settings.getPermissionRequirement("warn"))
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> warn(ctx.getSource(),
                                        GameProfileArgument.getGameProfiles(ctx, "player"),
                                        StringArgumentType.getString(ctx, "reason"))))));
    }

    private static int warn(CommandSourceStack source, Collection<NameAndId> targets, String input) {
        if (targets.size() != 1) {
            source.sendFailure(Messages.error("Please specify exactly one player."));
            return 0;
        }
        String reason = input.trim();
        if (reason.isEmpty()) {
            source.sendFailure(Messages.error("A warning reason is required."));
            return 0;
        }
        NameAndId target = targets.iterator().next();
        ModerationConfig config = ModerationConfig.get();
        try {
            WarningResult result = ModerationManager.get().warn(
                    target.id(), target.name(), reason, config.warningRollingPeriodMs,
                    BanCommand.moderator(source));
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(target.id());
            if (online != null) {
                online.sendSystemMessage(ModerationMessages.warning(result.warning()));
            }
            source.sendSuccess(() -> Messages.success(
                    "Warned " + target.name() + ": " + reason
                            + " (" + result.rollingCount() + " warnings in the rolling period)"), true);
            if (result.rollingCount() >= config.warningAlertThreshold) {
                source.sendSuccess(() -> Messages.info(
                        "Escalation alert: " + target.name() + " has " + result.rollingCount()
                                + " warnings in the configured rolling period."), false);
            }
            return 1;
        } catch (SQLException | IllegalStateException e) {
            Essentials.LOGGER.error("Failed to persist warning for {}", target.id(), e);
            source.sendFailure(Messages.error("The warning could not be saved."));
            return 0;
        }
    }
}

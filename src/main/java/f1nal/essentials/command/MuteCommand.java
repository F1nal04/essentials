package f1nal.essentials.command;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import f1nal.essentials.Essentials;
import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.moderation.BanDuration;
import f1nal.essentials.moderation.DurationParser;
import f1nal.essentials.moderation.ModerationManager;
import f1nal.essentials.moderation.ModerationMessages;
import f1nal.essentials.moderation.MuteRecord;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public final class MuteCommand {
    private MuteCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess, Commands.CommandSelection environment,
            CommandConfig.CommandSettings settings) {
        dispatcher.register(Commands.literal("mute")
                .requires(settings.getPermissionRequirement("mute"))
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(ctx -> mute(ctx.getSource(),
                                                GameProfileArgument.getGameProfiles(ctx, "player"),
                                                StringArgumentType.getString(ctx, "duration"),
                                                StringArgumentType.getString(ctx, "reason")))))));
    }

    private static int mute(CommandSourceStack source, Collection<NameAndId> targets,
            String durationText, String input) {
        if (targets.size() != 1) {
            source.sendFailure(Messages.error("Please specify exactly one player."));
            return 0;
        }
        String reason = input.trim();
        if (reason.isEmpty()) {
            source.sendFailure(Messages.error("A mute reason is required."));
            return 0;
        }
        BanDuration duration;
        try {
            duration = DurationParser.parseBanDuration(durationText);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Messages.error(e.getMessage()));
            return 0;
        }
        NameAndId target = targets.iterator().next();
        try {
            Optional<MuteRecord> result = ModerationManager.get().mute(
                    target.id(), target.name(), reason, duration, BanCommand.moderator(source));
            if (result.isEmpty()) {
                source.sendFailure(Messages.error(target.name() + " is already muted."));
                return 0;
            }
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(target.id());
            if (online != null) {
                online.sendSystemMessage(ModerationMessages.muted(
                        result.get(), ModerationManager.get().nowMs()));
            }
            source.sendSuccess(() -> Messages.success(
                    "Muted " + target.name() + " " + duration.commandDescription()
                            + ": " + reason), true);
            return 1;
        } catch (SQLException | IllegalStateException | IllegalArgumentException e) {
            Essentials.LOGGER.error("Failed to persist mute for {}", target.id(), e);
            source.sendFailure(Messages.error("The mute could not be saved."));
            return 0;
        }
    }
}

package f1nal.essentials.command;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import f1nal.essentials.Essentials;
import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.moderation.BanRecord;
import f1nal.essentials.moderation.BanDuration;
import f1nal.essentials.moderation.DurationParser;
import f1nal.essentials.moderation.ModerationManager;
import f1nal.essentials.moderation.ModerationMessages;
import f1nal.essentials.moderation.Moderator;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public final class BanCommand {

    private BanCommand() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess,
            Commands.CommandSelection environment,
            CommandConfig.CommandSettings settings) {
        dispatcher.register(Commands.literal("ban")
                .requires(settings.getPermissionRequirement())
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .then(Commands.argument("duration", StringArgumentType.word())
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(ctx -> ban(
                                                ctx.getSource(),
                                                GameProfileArgument.getGameProfiles(ctx, "player"),
                                                StringArgumentType.getString(ctx, "duration"),
                                                StringArgumentType.getString(ctx, "reason")))))));
    }

    private static int ban(
            CommandSourceStack source,
            Collection<NameAndId> targets,
            String durationText,
            String reasonText) {
        if (targets.size() != 1) {
            source.sendFailure(Messages.error("Please specify exactly one player."));
            return 0;
        }

        String reason = reasonText.trim();
        if (reason.isEmpty()) {
            source.sendFailure(Messages.error("A ban reason is required."));
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
        Moderator moderator = moderator(source);
        Optional<BanRecord> result;
        try {
            result = ModerationManager.get().ban(
                    target.id(), target.name(), reason, duration, moderator);
        } catch (SQLException | IllegalStateException e) {
            Essentials.LOGGER.error("Failed to persist ban for {}", target.id(), e);
            source.sendFailure(Messages.error("The ban could not be saved; no ban was applied."));
            return 0;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Messages.error(e.getMessage()));
            return 0;
        }

        if (result.isEmpty()) {
            source.sendFailure(Messages.error(target.name() + " is already banned."));
            return 0;
        }

        BanRecord ban = result.get();
        ServerPlayer online = source.getServer().getPlayerList().getPlayer(target.id());
        if (online != null) {
            online.connection.disconnect(ModerationMessages.banDisconnect(
                    ban, ModerationManager.get().nowMs()));
        }
        source.sendSuccess(() -> Messages.success(
                "Banned " + target.name() + " "
                        + duration.commandDescription() + ": " + reason), true);
        return 1;
    }

    static Moderator moderator(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return new Moderator(player.getUUID(), player.getName().getString());
        }
        return new Moderator(null, source.getTextName());
    }
}

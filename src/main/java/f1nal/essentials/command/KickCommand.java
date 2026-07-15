package f1nal.essentials.command;

import java.sql.SQLException;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import f1nal.essentials.Essentials;
import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.moderation.ModerationManager;
import f1nal.essentials.moderation.ModerationMessages;
import f1nal.essentials.moderation.Moderator;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public final class KickCommand {

    private KickCommand() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess,
            Commands.CommandSelection environment,
            CommandConfig.CommandSettings settings) {
        dispatcher.register(Commands.literal("kick")
                .requires(settings.getPermissionRequirement())
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> kick(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player"),
                                        StringArgumentType.getString(ctx, "reason"))))));
    }

    private static int kick(CommandSourceStack source, ServerPlayer target, String reasonText) {
        String reason = reasonText.trim();
        if (reason.isEmpty()) {
            source.sendFailure(Messages.error("A kick reason is required."));
            return 0;
        }
        if (source.getServer().isSingleplayerOwner(target.nameAndId())) {
            source.sendFailure(Messages.error("The singleplayer owner cannot be kicked."));
            return 0;
        }

        Moderator moderator = BanCommand.moderator(source);
        boolean auditWritten = true;
        try {
            ModerationManager.get().logKick(
                    target.getUUID(), target.getName().getString(), reason, moderator);
        } catch (SQLException | IllegalStateException e) {
            auditWritten = false;
            Essentials.LOGGER.error("Failed to write kick audit record for {}", target.getUUID(), e);
        }

        target.connection.disconnect(ModerationMessages.kickDisconnect(
                target.getName().getString(), reason, moderator));
        source.sendSuccess(() -> Messages.success(
                "Kicked " + target.getName().getString() + ": " + reason), true);
        if (!auditWritten) {
            source.sendFailure(Messages.error(
                    "The player was kicked, but the required audit record could not be saved."));
        }
        return 1;
    }
}

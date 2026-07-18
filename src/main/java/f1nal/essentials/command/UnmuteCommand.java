package f1nal.essentials.command;

import java.sql.SQLException;
import java.util.Collection;

import com.mojang.brigadier.CommandDispatcher;

import f1nal.essentials.Essentials;
import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.moderation.ModerationManager;
import f1nal.essentials.moderation.ModerationMessages;
import f1nal.essentials.moderation.Moderator;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

public final class UnmuteCommand {
    private UnmuteCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess, Commands.CommandSelection environment,
            CommandConfig.CommandSettings settings) {
        dispatcher.register(Commands.literal("unmute")
                .requires(settings.getPermissionRequirement("unmute"))
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(ctx -> unmute(ctx.getSource(),
                                GameProfileArgument.getGameProfiles(ctx, "player")))));
    }

    private static int unmute(CommandSourceStack source, Collection<NameAndId> targets) {
        if (targets.size() != 1) {
            source.sendFailure(Messages.error("Please specify exactly one player."));
            return 0;
        }
        NameAndId target = targets.iterator().next();
        Moderator moderator = BanCommand.moderator(source);
        try {
            if (!ModerationManager.get().unmute(target.id(), moderator)) {
                source.sendFailure(Messages.error(target.name() + " has no active mute."));
                return 0;
            }
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(target.id());
            if (online != null) {
                online.sendSystemMessage(ModerationMessages.unmuted(target.name(), moderator));
            }
            source.sendSuccess(() -> Messages.success("Unmuted " + target.name() + "."), true);
            return 1;
        } catch (SQLException | IllegalStateException e) {
            Essentials.LOGGER.error("Failed to revoke mute for {}", target.id(), e);
            source.sendFailure(Messages.error("The mute could not be revoked."));
            return 0;
        }
    }
}

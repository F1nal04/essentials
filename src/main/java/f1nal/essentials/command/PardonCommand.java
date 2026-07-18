package f1nal.essentials.command;

import java.sql.SQLException;
import java.util.Collection;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;

import f1nal.essentials.Essentials;
import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.moderation.ModerationManager;
import f1nal.essentials.moderation.Moderator;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.players.NameAndId;

public final class PardonCommand {

    private PardonCommand() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess,
            Commands.CommandSelection environment,
            CommandConfig.CommandSettings settings) {
        LiteralCommandNode<CommandSourceStack> pardon = dispatcher.register(
                Commands.literal("pardon")
                        .requires(settings.getPermissionRequirement("pardon"))
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(ctx -> pardon(
                                        ctx.getSource(),
                                        GameProfileArgument.getGameProfiles(ctx, "player")))));

        dispatcher.register(Commands.literal("unban")
                .requires(settings.getPermissionRequirement("unban"))
                .redirect(pardon));
    }

    private static int pardon(
            CommandSourceStack source,
            Collection<NameAndId> targets) {
        if (targets.size() != 1) {
            source.sendFailure(Messages.error("Please specify exactly one player."));
            return 0;
        }

        NameAndId target = targets.iterator().next();
        Moderator moderator = BanCommand.moderator(source);
        boolean revoked;
        try {
            revoked = ModerationManager.get().pardon(target.id(), moderator);
        } catch (SQLException | IllegalStateException e) {
            Essentials.LOGGER.error("Failed to revoke ban for {}", target.id(), e);
            source.sendFailure(Messages.error("The ban could not be revoked; no change was made."));
            return 0;
        }

        if (!revoked) {
            source.sendFailure(Messages.error(target.name() + " is not currently banned."));
            return 0;
        }

        source.sendSuccess(() -> Messages.success("Unbanned " + target.name() + "."), true);
        return 1;
    }
}

package f1nal.essentials.command;

import java.sql.SQLException;
import java.util.Collection;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import f1nal.essentials.Essentials;
import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.moderation.ModerationManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.players.NameAndId;

public final class NoteCommand {
    private NoteCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess, Commands.CommandSelection environment,
            CommandConfig.CommandSettings settings) {
        dispatcher.register(Commands.literal("note")
                .requires(settings.getPermissionRequirement("note"))
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> note(ctx.getSource(),
                                        GameProfileArgument.getGameProfiles(ctx, "player"),
                                        StringArgumentType.getString(ctx, "text"))))));
    }

    private static int note(CommandSourceStack source, Collection<NameAndId> targets, String input) {
        if (targets.size() != 1) {
            source.sendFailure(Messages.error("Please specify exactly one player."));
            return 0;
        }
        String text = input.trim();
        if (text.isEmpty()) {
            source.sendFailure(Messages.error("Staff note text is required."));
            return 0;
        }
        NameAndId target = targets.iterator().next();
        try {
            ModerationManager.get().addStaffNote(
                    target.id(), target.name(), text, BanCommand.moderator(source));
            source.sendSuccess(() -> Messages.success(
                    "Added a private staff note for " + target.name() + "."), false);
            return 1;
        } catch (SQLException | IllegalStateException e) {
            Essentials.LOGGER.error("Failed to persist staff note for {}", target.id(), e);
            source.sendFailure(Messages.error("The staff note could not be saved."));
            return 0;
        }
    }
}

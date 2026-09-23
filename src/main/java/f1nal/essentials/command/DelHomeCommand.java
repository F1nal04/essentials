package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.config.HomeConfig;
import f1nal.essentials.home.HomeManager;
import f1nal.essentials.home.HomeMessages;
import f1nal.essentials.home.HomeNames;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class DelHomeCommand {
    private DelHomeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess, Commands.CommandSelection environment,
            CommandConfig.CommandSettings settings) {
        dispatcher.register(Commands.literal("delhome")
                .requires(settings.getPermissionRequirement("delhome"))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(HomeNameSuggestions.names())
                        .executes(DelHomeCommand::deleteHome)));
    }

    private static int deleteHome(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        HomeConfig config = HomeConfig.get();
        if (player == null) {
            source.sendFailure(HomeMessages.format(config.playerOnlyMessage));
            return 0;
        }
        String chosen = StringArgumentType.getString(context, "name").trim();
        if (HomeNames.validate(chosen, config.namePattern) != HomeNames.Validity.OK) {
            source.sendFailure(HomeMessages.format(config.invalidNameMessage));
            return 0;
        }
        String canonical = HomeNames.canonical(chosen);
        return switch (HomeManager.deleteHome(player, canonical)) {
            case DELETED -> {
                source.sendSuccess(() -> HomeMessages.format(
                        config.deletedMessage, canonical, null, null, null), false);
                yield 1;
            }
            case MISSING -> {
                source.sendFailure(HomeMessages.format(
                        config.missingMessage, canonical, null, null, null));
                yield 0;
            }
            case SAVE_FAILED -> {
                source.sendFailure(HomeMessages.format(config.saveFailedMessage));
                yield 0;
            }
        };
    }
}

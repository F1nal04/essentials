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

public final class SetHomeCommand {
    private SetHomeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess, Commands.CommandSelection environment,
            CommandConfig.CommandSettings settings) {
        dispatcher.register(Commands.literal("sethome")
                .requires(settings.getPermissionRequirement("sethome"))
                .executes(context -> setHome(context, null))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(HomeNameSuggestions.names())
                        .executes(context -> setHome(context,
                                StringArgumentType.getString(context, "name")))));
    }

    private static int setHome(CommandContext<CommandSourceStack> context, String rawName) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        HomeConfig config = HomeConfig.get();
        if (player == null) {
            source.sendFailure(HomeMessages.format(config.playerOnlyMessage));
            return 0;
        }
        String chosen = rawName == null || rawName.isBlank() ? config.defaultName : rawName.trim();
        if (HomeNames.validate(chosen, config.namePattern) != HomeNames.Validity.OK) {
            source.sendFailure(HomeMessages.format(config.invalidNameMessage));
            return 0;
        }
        String canonical = HomeNames.canonical(chosen);
        int limit = HomeManager.limitFor(source);
        return switch (HomeManager.setHome(player, canonical, limit)) {
            case CREATED -> {
                source.sendSuccess(() -> HomeMessages.format(
                        config.setMessage, canonical, null, null, null), false);
                yield 1;
            }
            case DUPLICATE -> {
                source.sendFailure(HomeMessages.format(
                        config.duplicateMessage, canonical, null, null, null));
                yield 0;
            }
            case LIMIT_REACHED -> {
                source.sendFailure(HomeMessages.format(
                        config.limitMessage, null, null, limit, null));
                yield 0;
            }
            case SAVE_FAILED -> {
                source.sendFailure(HomeMessages.format(config.saveFailedMessage));
                yield 0;
            }
        };
    }
}

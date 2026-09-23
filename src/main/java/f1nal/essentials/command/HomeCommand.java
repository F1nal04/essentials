package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.config.HomeConfig;
import f1nal.essentials.home.HomeManager;
import f1nal.essentials.home.HomeMessages;
import f1nal.essentials.home.HomeNames;
import f1nal.essentials.permission.EssentialsPermissions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class HomeCommand {
    private HomeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess, Commands.CommandSelection environment,
            CommandConfig.CommandSettings settings) {
        dispatcher.register(Commands.literal("home")
                .requires(settings.getPermissionRequirement("home"))
                .executes(context -> home(context, null))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(HomeNameSuggestions.names())
                        .executes(context -> home(context,
                                StringArgumentType.getString(context, "name")))));
    }

    private static int home(CommandContext<CommandSourceStack> context, String rawName) {
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
        boolean bypassWarmup = EssentialsPermissions.require(
                "home.bypass.warmup",
                Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).test(source);
        boolean bypassCooldown = EssentialsPermissions.require(
                "home.bypass.cooldown",
                Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).test(source);
        HomeManager.Result result = HomeManager.requestTeleport(
                player, HomeNames.canonical(chosen), bypassWarmup, bypassCooldown);
        HomeManager.sendResult(player, result);
        return switch (result.status()) {
            case TELEPORTED, WARMING_UP -> 1;
            default -> 0;
        };
    }
}

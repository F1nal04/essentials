package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.config.SpawnConfig;
import f1nal.essentials.permission.EssentialsPermissions;
import f1nal.essentials.spawn.SpawnManager;
import f1nal.essentials.spawn.SpawnMessages;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class SpawnCommand {
    private SpawnCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess, Commands.CommandSelection environment,
            CommandConfig.CommandSettings settings) {
        dispatcher.register(Commands.literal("spawn")
                .requires(settings.getPermissionRequirement("spawn"))
                .executes(SpawnCommand::spawn));
    }

    private static int spawn(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        SpawnConfig config = SpawnConfig.get();
        if (player == null) {
            source.sendFailure(SpawnMessages.format(config.playerOnlyMessage));
            return 0;
        }
        boolean bypassWarmup = EssentialsPermissions.require(
                "spawn.bypass.warmup",
                Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).test(source);
        boolean bypassCooldown = EssentialsPermissions.require(
                "spawn.bypass.cooldown",
                Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).test(source);
        SpawnManager.RequestResult result = SpawnManager.requestTeleport(
                player, bypassWarmup, bypassCooldown);
        SpawnManager.sendResult(player, result);
        return switch (result.status()) {
            case TELEPORTED, WARMING_UP -> 1;
            default -> 0;
        };
    }
}

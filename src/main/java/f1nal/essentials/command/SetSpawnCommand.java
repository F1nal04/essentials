package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.config.SpawnConfig;
import f1nal.essentials.spawn.SpawnManager;
import f1nal.essentials.spawn.SpawnMessages;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class SetSpawnCommand {
    private SetSpawnCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess, Commands.CommandSelection environment,
            CommandConfig.CommandSettings settings) {
        dispatcher.register(Commands.literal("setspawn")
                .requires(settings.getPermissionRequirement("setspawn"))
                .executes(SetSpawnCommand::setSpawn));
    }

    private static int setSpawn(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        SpawnConfig config = SpawnConfig.get();
        if (player == null) {
            source.sendFailure(SpawnMessages.format(config.playerOnlyMessage));
            return 0;
        }
        if (!SpawnManager.setSpawn(player)) {
            source.sendFailure(SpawnMessages.format(config.saveFailedMessage));
            return 0;
        }
        source.sendSuccess(() -> SpawnMessages.format(config.setMessage), true);
        return 1;
    }
}

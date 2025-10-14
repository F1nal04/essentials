package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import f1nal.essentials.Messages;
import f1nal.essentials.back.BackManager;
import f1nal.essentials.config.BackConfig;
import f1nal.essentials.config.CommandConfig;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class BackCommand {

    private BackCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment, CommandConfig.CommandSettings settings) {
        dispatcher.register(CommandManager.literal("back")
                .requires(settings.getPermissionRequirement())
                .executes(BackCommand::back));
    }

    private static int back(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Messages.error("You must be a player to use this command."));
            return 0;
        }
        boolean ok = BackManager.teleportBack(player);
        if (!ok) {
            int s = BackConfig.get().windowSeconds;
            source.sendError(Messages.error("No previous position available or time window expired (" + s + "s)."));
            return 0;
        }
        source.sendFeedback(() -> Messages.success("Teleported back to your previous position."), false);
        return 1;
    }
}

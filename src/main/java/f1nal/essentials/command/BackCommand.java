package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import f1nal.essentials.Messages;
import f1nal.essentials.back.BackManager;
import f1nal.essentials.config.BackConfig;
import f1nal.essentials.config.CommandConfig;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class BackCommand {

    private BackCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment, CommandConfig.CommandSettings settings) {
        dispatcher.register(Commands.literal("back")
                .requires(settings.getPermissionRequirement())
                .executes(BackCommand::back));
    }

    private static int back(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }
        boolean ok = BackManager.teleportBack(player);
        if (!ok) {
            int s = BackConfig.get().windowSeconds;
            source.sendFailure(Messages.error("No previous position available or time window expired (" + s + "s)."));
            return 0;
        }
        source.sendSuccess(() -> Messages.success("Teleported back to your previous position."), false);
        return 1;
    }
}

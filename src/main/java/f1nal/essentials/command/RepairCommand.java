package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class RepairCommand {

    private RepairCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment, CommandConfig.CommandSettings settings) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("repair")
                .requires(settings.getPermissionRequirement())
                .executes(ctx -> repair(ctx.getSource(), ctx.getSource().getPlayer()))
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .executes(ctx -> repair(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "target"))));

        dispatcher.register(root);
    }

    private static int repair(ServerCommandSource source, ServerPlayerEntity target) {
        if (target == null) {
            source.sendError(Messages.error("You must be a player to use this command."));
            return 0;
        }

        ItemStack stack = target.getMainHandStack();
        if (stack.isEmpty()) {
            source.sendError(Messages.error(target.getName().getString() + " has an empty main hand."));
            return 0;
        }

        if (!stack.isDamageable()) {
            source.sendFeedback(() -> Messages.warning("The item in main hand is not damageable: " + stack.getName().getString()), false);
            return 1;
        }

        stack.setDamage(0);

        if (source.getEntity() == target) {
            source.sendFeedback(() -> Messages.info("Repaired main-hand item: " + stack.getName().getString()), false);
        } else {
            source.sendFeedback(() -> Messages.info("Repaired " + target.getName().getString() + "'s main-hand item: " + stack.getName().getString()), true);
            target.sendMessage(Messages.info("Main-hand item was repaired by " + source.getName() + "."));
        }

        return 1;
    }
}

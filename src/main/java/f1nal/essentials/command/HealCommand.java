package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import f1nal.essentials.Messages;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public final class HealCommand {

    private HealCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("heal")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(ctx -> heal(ctx.getSource(), ctx.getSource().getPlayer()))
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .executes(ctx -> heal(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "target"))));

        dispatcher.register(root);
    }

    private static int heal(ServerCommandSource source, ServerPlayerEntity target) {
        if (target == null) {
            source.sendError(Messages.error("You must be a player to use this command."));
            return 0;
        }

        float maxHealth = target.getMaxHealth();
        target.setHealth(maxHealth);
        target.getHungerManager().setFoodLevel(20);
        target.getHungerManager().setSaturationLevel(20.0F);

        if (source.getEntity() == target) {
            source.sendFeedback(() -> Messages.info("Healed to full health (" + maxHealth + ")"), false);
        } else {
            source.sendFeedback(() -> Messages.info("Healed " + target.getName().getString() + " to full health (" + maxHealth + ")"), true);
            target.sendMessage(Messages.info("Healed to full health by " + source.getName() + "."));
        }

        return 1;
    }
}

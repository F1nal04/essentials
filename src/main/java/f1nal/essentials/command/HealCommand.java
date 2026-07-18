package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public final class HealCommand {

    private HealCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment, CommandConfig.CommandSettings settings) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("heal")
                .requires(settings.getPermissionRequirement("heal"))
                .executes(ctx -> heal(ctx.getSource(), ctx.getSource().getPlayer()))
                .then(Commands.argument("target", EntityArgument.player())
                        .requires(settings.getPermissionRequirement("heal.others"))
                        .executes(ctx -> heal(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"))));

        dispatcher.register(root);
    }

    private static int heal(CommandSourceStack source, ServerPlayer target) {
        if (target == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }

        float maxHealth = target.getMaxHealth();
        target.setHealth(maxHealth);
        target.getFoodData().setFoodLevel(20);
        target.getFoodData().setSaturation(20.0F);

        if (source.getEntity() == target) {
            source.sendSuccess(() -> Messages.info("Healed to full health (" + maxHealth + ")"), false);
        } else {
            source.sendSuccess(() -> Messages.info("Healed " + target.getName().getString() + " to full health (" + maxHealth + ")"), true);
            target.sendSystemMessage(Messages.info("Healed to full health by " + source.getTextName() + "."));
        }

        return 1;
    }
}

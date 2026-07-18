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
import net.minecraft.world.item.ItemStack;

public final class RepairCommand {

    private RepairCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment, CommandConfig.CommandSettings settings) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("repair")
                .requires(settings.getPermissionRequirement("repair"))
                .executes(ctx -> repair(ctx.getSource(), ctx.getSource().getPlayer()))
                .then(Commands.argument("target", EntityArgument.player())
                        .requires(settings.getPermissionRequirement("repair.others"))
                        .executes(ctx -> repair(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"))));

        dispatcher.register(root);
    }

    private static int repair(CommandSourceStack source, ServerPlayer target) {
        if (target == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }

        ItemStack stack = target.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Messages.error(target.getName().getString() + " has an empty main hand."));
            return 0;
        }

        if (!stack.isDamageableItem()) {
            source.sendSuccess(() -> Messages.warning("The item in main hand is not damageable: " + stack.getHoverName().getString()), false);
            return 1;
        }

        stack.setDamageValue(0);

        if (source.getEntity() == target) {
            source.sendSuccess(() -> Messages.info("Repaired main-hand item: " + stack.getHoverName().getString()), false);
        } else {
            source.sendSuccess(() -> Messages.info("Repaired " + target.getName().getString() + "'s main-hand item: " + stack.getHoverName().getString()), true);
            target.sendSystemMessage(Messages.info("Main-hand item was repaired by " + source.getTextName() + "."));
        }

        return 1;
    }
}

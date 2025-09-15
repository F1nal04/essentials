package f1nal.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Essentials implements ModInitializer {
    public static final String MOD_ID = "essentials";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        registerCommands();
        LOGGER.info("Essentials initialized");
    }

    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register(this::registerRepairCommand);
        CommandRegistrationCallback.EVENT.register(this::registerHealCommand);
        CommandRegistrationCallback.EVENT.register(this::registerFeedCommand);
    }

    private void registerRepairCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("repair")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(ctx -> repair(ctx.getSource(), ctx.getSource().getPlayer()))
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .executes(ctx -> repair(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "target"))));

        dispatcher.register(root);
    }

    private int repair(ServerCommandSource source, ServerPlayerEntity target) {
        if (target == null) {
            source.sendError(Text.literal("You must be a player to use this command without a target."));
            return 0;
        }

        ItemStack stack = target.getMainHandStack();
        if (stack.isEmpty()) {
            source.sendError(Text.literal(target.getName().getString() + " has an empty main hand."));
            return 0;
        }

        if (!stack.isDamageable()) {
            source.sendFeedback(() -> Text.literal("The item in main hand is not damageable: " + stack.getName().getString()), false);
            return 1;
        }

        // Set damage to 0 to fully repair
        stack.setDamage(0);

        if (source.getEntity() == target) {
            source.sendFeedback(() -> Text.literal("Repaired your main-hand item: " + stack.getName().getString()), false);
        } else {
            source.sendFeedback(() -> Text.literal("Repaired " + target.getName().getString() + "'s main-hand item: " + stack.getName().getString()), true);
            target.sendMessage(Text.literal("Your main-hand item was repaired by " + source.getName() + "."));
        }

        return 1;
    }

    private void registerHealCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("heal")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(ctx -> heal(ctx.getSource(), ctx.getSource().getPlayer()))
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .executes(ctx -> heal(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "target"))));

        dispatcher.register(root);
    }

    private int heal(ServerCommandSource source, ServerPlayerEntity target) {
        if (target == null) {
            source.sendError(Text.literal("You must be a player to use this command without a target."));
            return 0;
        }

        float maxHealth = target.getMaxHealth();
        target.setHealth(maxHealth);
        target.getHungerManager().setFoodLevel(20);
        target.getHungerManager().setSaturationLevel(20.0F);

        if (source.getEntity() == target) {
            source.sendFeedback(() -> Text.literal("Healed you to full health (" + maxHealth + ")"), false);
        } else {
            source.sendFeedback(() -> Text.literal("Healed " + target.getName().getString() + " to full health (" + maxHealth + ")"), true);
            target.sendMessage(Text.literal("You were healed by " + source.getName() + "."));
        }

        return 1;
    }

    private void registerFeedCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        LiteralArgumentBuilder<ServerCommandSource> root = CommandManager.literal("feed")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(ctx -> feed(ctx.getSource(), ctx.getSource().getPlayer()))
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .executes(ctx -> feed(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "target"))));

        dispatcher.register(root);
    }

    private int feed(ServerCommandSource source, ServerPlayerEntity target) {
        if (target == null) {
            source.sendError(Text.literal("You must be a player to use this command without a target."));
            return 0;
        }

        target.getHungerManager().setFoodLevel(20);
        target.getHungerManager().setSaturationLevel(20.0F);

        if (source.getEntity() == target) {
            source.sendFeedback(() -> Text.literal("Fed you to full hunger and saturation."), false);
        } else {
            source.sendFeedback(() -> Text.literal("Fed " + target.getName().getString() + " to full hunger and saturation."), true);
            target.sendMessage(Text.literal("You were fed by " + source.getName() + "."));
        }

        return 1;
    }
}
package f1nal.essentials.command;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig.CommandSettings;
import f1nal.essentials.config.MessagingConfig;
import f1nal.essentials.messaging.MessageFormatter;
import f1nal.essentials.messaging.MessagingManager;
import f1nal.essentials.messaging.MessagingState;
import f1nal.essentials.moderation.MuteEnforcement;
import f1nal.essentials.permission.EssentialsPermissions;
import f1nal.essentials.vanish.VanishManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** /msg, /r, /ignore, /msgspy and /msgall. */
public final class MessageCommands {
    private static CommandSettings spySettings;

    private MessageCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandSettings msg, CommandSettings reply, CommandSettings ignore,
            CommandSettings spy, CommandSettings all) {
        spySettings = spy;

        if (msg.enabled()) {
            dispatcher.register(messageLiteral("msg", msg));
            dispatcher.register(messageLiteral("tell", msg));
            dispatcher.register(messageLiteral("w", msg));
        }
        if (reply.enabled()) {
            dispatcher.register(Commands.literal("r")
                    .requires(reply.getPermissionRequirement("reply"))
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                            .executes(MessageCommands::reply)));
        }
        if (ignore.enabled()) {
            dispatcher.register(Commands.literal("ignore")
                    .requires(ignore.getPermissionRequirement("ignore"))
                    .then(Commands.argument("player", StringArgumentType.word())
                            .suggests(MessageCommands::suggestPlayers)
                            .executes(MessageCommands::toggleIgnore)));
        }
        if (spy.enabled()) {
            dispatcher.register(Commands.literal("msgspy")
                    .requires(spy.getPermissionRequirement("msgspy"))
                    .executes(MessageCommands::toggleSpy));
        }
        if (all.enabled()) {
            dispatcher.register(Commands.literal("msgall")
                    .requires(all.getPermissionRequirement("msgall"))
                    .then(Commands.argument("message", StringArgumentType.greedyString())
                            .executes(MessageCommands::messageAll)));
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> messageLiteral(
            String name, CommandSettings settings) {
        return Commands.literal(name)
                .requires(settings.getPermissionRequirement("msg"))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(MessageCommands::suggestPlayers)
                        .then(Commands.argument("message", StringArgumentType.greedyString())
                                .executes(MessageCommands::message)));
    }

    private static int message(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!MuteEnforcement.allowPrivateMessage(source)) return 0;
        ServerPlayer target = resolveVisible(source,
                StringArgumentType.getString(context, "player"));
        if (target == null) return unavailable(source);
        return deliver(source, target, StringArgumentType.getString(context, "message"), false);
    }

    private static int reply(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer sender = source.getPlayer();
        if (sender == null) {
            source.sendFailure(Messages.error("The server console cannot use /r."));
            return 0;
        }
        if (!MuteEnforcement.allowPrivateMessage(source)) return 0;
        UUID targetId = MessagingManager.state().replyTarget(sender.getUUID()).orElse(null);
        if (targetId == null) {
            source.sendFailure(MessageFormatter.format(
                    MessagingConfig.get().missingReplyTargetFormat, sender.getName().getString(), "", ""));
            return 0;
        }
        String message = StringArgumentType.getString(context, "message");
        if (MessagingState.CONSOLE_ID.equals(targetId)) {
            return deliverToConsole(source, sender, message);
        }
        ServerPlayer target = source.getServer().getPlayerList().getPlayer(targetId);
        if (target == null || !canSee(source, target)) return unavailable(source);
        return deliver(source, target, message, true);
    }

    private static int deliver(CommandSourceStack source, ServerPlayer recipient,
            String message, boolean reply) {
        ServerPlayer sender = source.getPlayer();
        String senderName = sender == null ? "Console" : sender.getName().getString();
        String recipientName = recipient.getName().getString();
        MessagingConfig config = MessagingConfig.get();
        if (sender != null && sender.getUUID().equals(recipient.getUUID())) {
            source.sendFailure(Messages.error("You cannot message yourself."));
            return 0;
        }
        if (sender != null
                && MessagingManager.ignores().isIgnoring(recipient.getUUID(), sender.getUUID())
                && !bypassesIgnore(source)) {
            source.sendFailure(MessageFormatter.format(
                    config.ignoredPlayerFormat, senderName, recipientName, message));
            return 0;
        }

        recipient.sendSystemMessage(MessageFormatter.format(
                config.incomingFormat, senderName, recipientName, message));
        source.sendSuccess(() -> MessageFormatter.format(
                reply ? config.replyFormat : config.outgoingFormat,
                senderName, recipientName, message), false);
        if (sender != null) {
            MessagingManager.state().recordConversation(sender.getUUID(), recipient.getUUID());
            // Keep private-message traffic visible to administrators even when
            // no online staff member has enabled /msgspy.
            source.getServer().sendSystemMessage(MessageFormatter.format(
                    config.spyFormat, senderName, recipientName, message));
        } else {
            MessagingManager.state().recordConsoleMessage(recipient.getUUID());
        }
        sendSpyMessage(source.getServer(), sender, recipient, senderName, recipientName, message);
        return 1;
    }

    private static int deliverToConsole(CommandSourceStack source, ServerPlayer sender,
            String message) {
        String senderName = sender.getName().getString();
        MessagingConfig config = MessagingConfig.get();
        source.getServer().sendSystemMessage(MessageFormatter.format(
                config.incomingFormat, senderName, "Console", message));
        source.sendSuccess(() -> MessageFormatter.format(
                config.replyFormat, senderName, "Console", message), false);
        MessagingManager.state().recordConsoleMessage(sender.getUUID());
        sendSpyMessage(source.getServer(), sender, null,
                senderName, "Console", message);
        return 1;
    }

    private static int toggleIgnore(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer owner = source.getPlayer();
        if (owner == null) {
            source.sendFailure(Messages.error("The server console cannot ignore players."));
            return 0;
        }
        ServerPlayer target = resolveVisible(source,
                StringArgumentType.getString(context, "player"));
        if (target == null) return unavailable(source);
        if (owner.getUUID().equals(target.getUUID())) {
            source.sendFailure(Messages.error("You cannot ignore yourself."));
            return 0;
        }
        try {
            boolean ignored = MessagingManager.ignores().toggle(owner.getUUID(), target.getUUID());
            source.sendSuccess(() -> ignored
                    ? Messages.success("You are now ignoring " + target.getName().getString() + ".")
                    : Messages.info("You are no longer ignoring " + target.getName().getString() + "."), false);
            return 1;
        } catch (IOException e) {
            source.sendFailure(Messages.error("Could not save your ignore setting."));
            return 0;
        }
    }

    private static int toggleSpy(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Messages.error("The server console cannot toggle message spy."));
            return 0;
        }
        boolean enabled = MessagingManager.state().toggleSpy(player.getUUID());
        context.getSource().sendSuccess(() -> enabled
                ? Messages.success("Private-message spy enabled.")
                : Messages.info("Private-message spy disabled."), false);
        return 1;
    }

    private static int messageAll(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!MuteEnforcement.allowPrivateMessage(source)) return 0;
        String message = StringArgumentType.getString(context, "message");
        MessagingConfig config = MessagingConfig.get();
        String sender = source.getPlayer() == null ? "Console" : source.getPlayer().getName().getString();
        int count = 0;
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            player.sendSystemMessage(MessageFormatter.format(
                    config.messageAllFormat, sender, player.getName().getString(), message));
            count++;
        }
        source.getServer().sendSystemMessage(MessageFormatter.format(
                config.messageAllFormat, sender, "Console", message));
        int sent = count;
        source.sendSuccess(() -> Messages.success("Message sent to " + sent + " online player(s)."), false);
        return count;
    }

    private static void sendSpyMessage(MinecraftServer server, ServerPlayer sender,
            ServerPlayer recipient, String senderName, String recipientName, String message) {
        if (spySettings == null || !spySettings.enabled()) return;
        for (ServerPlayer spy : server.getPlayerList().getPlayers()) {
            if (spy == sender || spy == recipient || !MessagingManager.state().isSpying(spy.getUUID())) continue;
            if (!spySettings.getPermissionRequirement("msgspy").test(spy.createCommandSourceStack())) {
                MessagingManager.state().removeSpy(spy.getUUID());
                continue;
            }
            spy.sendSystemMessage(MessageFormatter.format(
                    MessagingConfig.get().spyFormat, senderName, recipientName, message));
        }
    }

    private static ServerPlayer resolveVisible(CommandSourceStack source, String name) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(name);
        return player != null && canSee(source, player) ? player : null;
    }

    private static boolean canSee(CommandSourceStack source, ServerPlayer target) {
        return VanishManager.canSee(source, target);
    }

    private static boolean bypassesIgnore(CommandSourceStack source) {
        MessagingConfig config = MessagingConfig.get();
        if (source.getPlayer() == null) return config.consoleBypassesIgnore;
        return config.staffBypassesIgnore
                && EssentialsPermissions.require("msg.ignore.bypass",
                        Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).test(source);
    }

    private static int unavailable(CommandSourceStack source) {
        source.sendFailure(MessageFormatter.format(
                MessagingConfig.get().unavailableTargetFormat, source.getTextName(), "", ""));
        return 0;
    }

    private static java.util.concurrent.CompletableFuture<Suggestions> suggestPlayers(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            String name = player.getName().getString();
            if (name.toLowerCase(Locale.ROOT).startsWith(remaining)
                    && canSee(context.getSource(), player)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }
}

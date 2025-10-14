package f1nal.essentials.command;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.tpa.TpaManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class TpaCommands {

    private TpaCommands() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment, CommandConfig.CommandSettings settings) {
        LiteralArgumentBuilder<ServerCommandSource> tpa = CommandManager.literal("tpa")
                .requires(settings.getPermissionRequirement())
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .executes(ctx -> sendTpa(ctx, EntityArgumentType.getPlayer(ctx, "target"))));

        LiteralArgumentBuilder<ServerCommandSource> tpahere = CommandManager.literal("tpahere")
                .requires(settings.getPermissionRequirement())
                .then(CommandManager.literal("all")
                        .executes(TpaCommands::sendTpahereAll))
                .then(CommandManager.argument("target", EntityArgumentType.player())
                        .executes(ctx -> sendTpahere(ctx, EntityArgumentType.getPlayer(ctx, "target"))));

        LiteralArgumentBuilder<ServerCommandSource> tpaccept = CommandManager.literal("tpaccept")
                .requires(settings.getPermissionRequirement())
                .executes(ctx -> accept(ctx, null))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ctx -> accept(ctx, EntityArgumentType.getPlayer(ctx, "player"))));

        LiteralArgumentBuilder<ServerCommandSource> tpdeny = CommandManager.literal("tpdeny")
                .requires(settings.getPermissionRequirement())
                .executes(ctx -> deny(ctx, null))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .executes(ctx -> deny(ctx, EntityArgumentType.getPlayer(ctx, "player"))));

        LiteralArgumentBuilder<ServerCommandSource> tpcancel = CommandManager.literal("tpcancel")
                .requires(settings.getPermissionRequirement())
                .executes(TpaCommands::cancel);

        dispatcher.register(tpa);
        dispatcher.register(tpahere);
        dispatcher.register(tpaccept);
        dispatcher.register(tpdeny);
        dispatcher.register(tpcancel);
    }

    private static int sendTpa(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity sender = source.getPlayer();
        if (sender == null) {
            source.sendError(Messages.error("You must be a player to use this command."));
            return 0;
        }
        if (sender == target) {
            source.sendError(Messages.error("You cannot send a TPA request to yourself."));
            return 0;
        }

        Optional<Long> cd = TpaManager.getSecondsLeftOnCancelCooldown(sender);
        if (cd.isPresent()) {
            source.sendError(Messages.error("You must wait " + cd.get() + "s before sending another request."));
            return 0;
        }

        boolean created = TpaManager.createRequest(sender, target, TpaManager.Type.TPA);
        if (!created) {
            source.sendError(Messages.error("You already have a pending request."));
            return 0;
        }

        source.sendFeedback(() -> Messages.info("TPA request sent to " + target.getName().getString() + "."), false);
        sendButtonsToTarget(target, sender, false);
        return 1;
    }

    private static int sendTpahere(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity target) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity sender = source.getPlayer();
        if (sender == null) {
            source.sendError(Messages.error("You must be a player to use this command."));
            return 0;
        }
        if (sender == target) {
            source.sendError(Messages.error("You cannot send a TPAHere request to yourself."));
            return 0;
        }

        Optional<Long> cd = TpaManager.getSecondsLeftOnCancelCooldown(sender);
        if (cd.isPresent()) {
            source.sendError(Messages.error("You must wait " + cd.get() + "s before sending another request."));
            return 0;
        }

        boolean created = TpaManager.createRequest(sender, target, TpaManager.Type.TPA_HERE);
        if (!created) {
            source.sendError(Messages.error("You already have a pending request."));
            return 0;
        }
        source.sendFeedback(() -> Messages.info("TPAHere request sent to " + target.getName().getString() + "."), false);
        sendButtonsToTarget(target, sender, true);
        return 1;
    }

    private static int sendTpahereAll(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity sender = source.getPlayer();
        if (sender == null) {
            source.sendError(Messages.error("You must be a player to use this command."));
            return 0;
        }
        Optional<Long> cd = TpaManager.getSecondsLeftOnCancelCooldown(sender);
        if (cd.isPresent()) {
            source.sendError(Messages.error("You must wait " + cd.get() + "s before sending another request."));
            return 0;
        }
        MinecraftServer server = sender.getServer();
        if (server == null) {
            return 0;
        }
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        int count = 0;
        for (ServerPlayerEntity p : players) {
            if (p == sender) {
                continue;
            }
            if (TpaManager.createRequest(sender, p, TpaManager.Type.TPA_HERE)) {
                sendButtonsToTarget(p, sender, true);
                count++;
            }
        }
        if (count == 0) {
            source.sendError(Messages.error("Failed to create any requests (you may already have one pending)."));
            return 0;
        }
        final int cnt = count;
        source.sendFeedback(() -> Messages.info("TPAHere request sent to " + cnt + " player(s)."), false);
        return 1;
    }

    private static int accept(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity from) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity target = source.getPlayer();
        if (target == null) {
            source.sendError(Messages.error("You must be a player to use this command."));
            return 0;
        }
        Optional<TpaManager.Request> reqOpt = TpaManager.accept(target, from);
        if (reqOpt.isEmpty()) {
            source.sendError(Messages.error("No pending request found."));
            return 0;
        }
        TpaManager.Request req = reqOpt.get();
        MinecraftServer server = target.getServer();
        if (server == null) {
            return 0;
        }
        ServerPlayerEntity sender = server.getPlayerManager().getPlayer(req.sender);
        if (sender == null) {
            source.sendError(Messages.error("The requester is no longer online."));
            return 0;
        }

        // Perform teleport
        if (req.type == TpaManager.Type.TPA) {
            // sender -> target
            sender.teleport(target.getWorld(), target.getX(), target.getY(), target.getZ(),
                    Set.of(),
                    target.getYaw(), target.getPitch(), false);
        } else {
            // target -> sender
            target.teleport(sender.getWorld(), sender.getX(), sender.getY(), sender.getZ(),
                    Set.of(),
                    sender.getYaw(), sender.getPitch(), false);
        }

        sender.sendMessage(Messages.success("Teleport request accepted by " + target.getName().getString() + "."));
        target.sendMessage(Messages.success("Teleport request accepted."));
        return 1;
    }

    private static int deny(CommandContext<ServerCommandSource> ctx, ServerPlayerEntity from) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity target = source.getPlayer();
        if (target == null) {
            source.sendError(Messages.error("You must be a player to use this command."));
            return 0;
        }
        Optional<TpaManager.Request> reqOpt = TpaManager.deny(target, from);
        if (reqOpt.isEmpty()) {
            source.sendError(Messages.error("No pending request found."));
            return 0;
        }
        TpaManager.Request req = reqOpt.get();
        MinecraftServer server = target.getServer();
        if (server != null) {
            ServerPlayerEntity sender = server.getPlayerManager().getPlayer(req.sender);
            if (sender != null) {
                sender.sendMessage(Messages.warning(target.getName().getString() + " declined your teleport request."));
            }
        }
        target.sendMessage(Messages.info("Teleport request declined."));
        return 1;
    }

    private static int cancel(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity sender = source.getPlayer();
        if (sender == null) {
            source.sendError(Messages.error("You must be a player to use this command."));
            return 0;
        }
        boolean cancelled = TpaManager.cancel(sender);
        if (!cancelled) {
            source.sendError(Messages.error("You have no pending request to cancel."));
            return 0;
        }
        source.sendFeedback(() -> Messages.info("Teleport request cancelled. You must wait 10s before sending another."), false);
        // Notify target if online
        MinecraftServer server = sender.getServer();
        if (server != null) {
            // There's at most one outgoing, but we no longer have access to the req here
            // so we can't precisely notify. This is acceptable minimalism.
        }
        return 1;
    }

    private static void sendButtonsToTarget(ServerPlayerEntity target, ServerPlayerEntity sender, boolean here) {
        String senderName = sender.getName().getString();
        String typeText = here ? "wants you to teleport to them" : "wants to teleport to you";
        MutableText header = Text.empty()
                .append(Text.literal(senderName).formatted(Formatting.WHITE))
                .append(Text.literal(" " + typeText + ". ").formatted(Formatting.GRAY));

        MutableText acceptBtn = Text.literal("[Accept]")
                .formatted(Formatting.GREEN)
                .styled(s -> s
                .withBold(true)
                .withClickEvent(new ClickEvent.RunCommand("/tpaccept " + senderName))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to accept " + senderName + "'s request").formatted(Formatting.GRAY))))
                .append(Text.literal(" "));

        MutableText denyBtn = Text.literal("[Decline]")
                .formatted(Formatting.RED)
                .styled(s -> s
                .withBold(true)
                .withClickEvent(new ClickEvent.RunCommand("/tpdeny " + senderName))
                .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click to decline " + senderName + "'s request").formatted(Formatting.GRAY))));

        MutableText hint = Text.literal("Use /tpaccept " + senderName + " or /tpdeny " + senderName)
                .formatted(Formatting.DARK_GRAY);

        target.sendMessage(Messages.custom(header.append(Text.literal("\n")).append(acceptBtn).append(denyBtn).append(Text.literal("\n")).append(hint)));
    }
}

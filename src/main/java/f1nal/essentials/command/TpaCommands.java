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
import f1nal.essentials.back.BackManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class TpaCommands {

    private TpaCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment, CommandConfig.CommandSettings settings) {
        LiteralArgumentBuilder<CommandSourceStack> tpa = Commands.literal("tpa")
                .requires(settings.getPermissionRequirement())
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> sendTpa(ctx, EntityArgument.getPlayer(ctx, "target"))));

        LiteralArgumentBuilder<CommandSourceStack> tpahere = Commands.literal("tpahere")
                .requires(settings.getPermissionRequirement())
                .then(Commands.literal("all")
                        .executes(TpaCommands::sendTpahereAll))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> sendTpahere(ctx, EntityArgument.getPlayer(ctx, "target"))));

        LiteralArgumentBuilder<CommandSourceStack> tpaccept = Commands.literal("tpaccept")
                .requires(settings.getPermissionRequirement())
                .executes(ctx -> accept(ctx, null))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> accept(ctx, EntityArgument.getPlayer(ctx, "player"))));

        LiteralArgumentBuilder<CommandSourceStack> tpdeny = Commands.literal("tpdeny")
                .requires(settings.getPermissionRequirement())
                .executes(ctx -> deny(ctx, null))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> deny(ctx, EntityArgument.getPlayer(ctx, "player"))));

        LiteralArgumentBuilder<CommandSourceStack> tpcancel = Commands.literal("tpcancel")
                .requires(settings.getPermissionRequirement())
                .executes(TpaCommands::cancel);

        dispatcher.register(tpa);
        dispatcher.register(tpahere);
        dispatcher.register(tpaccept);
        dispatcher.register(tpdeny);
        dispatcher.register(tpcancel);
    }

    private static int sendTpa(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer sender = source.getPlayer();
        if (sender == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }
        if (sender == target) {
            source.sendFailure(Messages.error("You cannot send a TPA request to yourself."));
            return 0;
        }

        Optional<Long> cd = TpaManager.getSecondsLeftOnCancelCooldown(sender);
        if (cd.isPresent()) {
            source.sendFailure(Messages.error("You must wait " + cd.get() + "s before sending another request."));
            return 0;
        }

        boolean created = TpaManager.createRequest(sender, target, TpaManager.Type.TPA);
        if (!created) {
            source.sendFailure(Messages.error("You already have a pending request."));
            return 0;
        }

        source.sendSuccess(() -> Messages.info("TPA request sent to " + target.getName().getString() + "."), false);
        sendButtonsToTarget(target, sender, false);
        return 1;
    }

    private static int sendTpahere(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer sender = source.getPlayer();
        if (sender == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }
        if (sender == target) {
            source.sendFailure(Messages.error("You cannot send a TPAHere request to yourself."));
            return 0;
        }

        Optional<Long> cd = TpaManager.getSecondsLeftOnCancelCooldown(sender);
        if (cd.isPresent()) {
            source.sendFailure(Messages.error("You must wait " + cd.get() + "s before sending another request."));
            return 0;
        }

        boolean created = TpaManager.createRequest(sender, target, TpaManager.Type.TPA_HERE);
        if (!created) {
            source.sendFailure(Messages.error("You already have a pending request."));
            return 0;
        }
        source.sendSuccess(() -> Messages.info("TPAHere request sent to " + target.getName().getString() + "."), false);
        sendButtonsToTarget(target, sender, true);
        return 1;
    }

    private static int sendTpahereAll(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer sender = source.getPlayer();
        if (sender == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }
        Optional<Long> cd = TpaManager.getSecondsLeftOnCancelCooldown(sender);
        if (cd.isPresent()) {
            source.sendFailure(Messages.error("You must wait " + cd.get() + "s before sending another request."));
            return 0;
        }
        MinecraftServer server = sender.level().getServer();
        if (server == null) {
            return 0;
        }
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        int count = 0;
        for (ServerPlayer p : players) {
            if (p == sender) {
                continue;
            }
            if (TpaManager.createRequest(sender, p, TpaManager.Type.TPA_HERE)) {
                sendButtonsToTarget(p, sender, true);
                count++;
            }
        }
        if (count == 0) {
            source.sendFailure(Messages.error("Failed to create any requests (you may already have one pending)."));
            return 0;
        }
        final int cnt = count;
        source.sendSuccess(() -> Messages.info("TPAHere request sent to " + cnt + " player(s)."), false);
        return 1;
    }

    private static int accept(CommandContext<CommandSourceStack> ctx, ServerPlayer from) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer target = source.getPlayer();
        if (target == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }
        Optional<TpaManager.Request> reqOpt = TpaManager.accept(target, from);
        if (reqOpt.isEmpty()) {
            source.sendFailure(Messages.error("No pending request found."));
            return 0;
        }
        TpaManager.Request req = reqOpt.get();
        MinecraftServer server = target.level().getServer();
        if (server == null) {
            return 0;
        }
        ServerPlayer sender = server.getPlayerList().getPlayer(req.sender);
        if (sender == null) {
            source.sendFailure(Messages.error("The requester is no longer online."));
            return 0;
        }

        if (req.type == TpaManager.Type.TPA) {
            // sender -> target
            BackManager.markBackPosition(sender); // record sender's previous position
            sender.teleportTo(target.level(), target.getX(), target.getY(), target.getZ(),
                    Set.of(),
                    target.getYRot(), target.getXRot(), false);
        } else {
            // target -> sender
            BackManager.markBackPosition(target); // record target's previous position
            target.teleportTo(sender.level(), sender.getX(), sender.getY(), sender.getZ(),
                    Set.of(),
                    sender.getYRot(), sender.getXRot(), false);
        }

        sender.sendSystemMessage(Messages.success("Teleport request accepted by " + target.getName().getString() + "."));
        target.sendSystemMessage(Messages.success("Teleport request accepted."));
        return 1;
    }

    private static int deny(CommandContext<CommandSourceStack> ctx, ServerPlayer from) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer target = source.getPlayer();
        if (target == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }
        Optional<TpaManager.Request> reqOpt = TpaManager.deny(target, from);
        if (reqOpt.isEmpty()) {
            source.sendFailure(Messages.error("No pending request found."));
            return 0;
        }
        TpaManager.Request req = reqOpt.get();
        MinecraftServer server = target.level().getServer();
        if (server != null) {
            ServerPlayer sender = server.getPlayerList().getPlayer(req.sender);
            if (sender != null) {
                sender.sendSystemMessage(Messages.warning(target.getName().getString() + " declined your teleport request."));
            }
        }
        target.sendSystemMessage(Messages.info("Teleport request declined."));
        return 1;
    }

    private static int cancel(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer sender = source.getPlayer();
        if (sender == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }
        boolean cancelled = TpaManager.cancel(sender);
        if (!cancelled) {
            source.sendFailure(Messages.error("You have no pending request to cancel."));
            return 0;
        }
        int cd = f1nal.essentials.config.TpaConfig.get().cooldownSeconds;
        source.sendSuccess(() -> Messages.info("Teleport request cancelled. You must wait " + cd + "s before sending another."), false);

        return 1;
    }

    private static void sendButtonsToTarget(ServerPlayer target, ServerPlayer sender, boolean here) {
        String senderName = sender.getName().getString();
        String typeText = here ? "wants you to teleport to them" : "wants to teleport to you";
        MutableComponent header = Component.empty()
                .append(Component.literal(senderName).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" " + typeText + ". ").withStyle(ChatFormatting.GRAY));

        MutableComponent acceptBtn = Component.literal("[Accept]")
                .withStyle(ChatFormatting.GREEN)
                .withStyle(s -> s
                .withBold(true)
                .withClickEvent(new ClickEvent.RunCommand("/tpaccept " + senderName))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to accept " + senderName + "'s request").withStyle(ChatFormatting.GRAY))))
                .append(Component.literal(" "));

        MutableComponent denyBtn = Component.literal("[Decline]")
                .withStyle(ChatFormatting.RED)
                .withStyle(s -> s
                .withBold(true)
                .withClickEvent(new ClickEvent.RunCommand("/tpdeny " + senderName))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to decline " + senderName + "'s request").withStyle(ChatFormatting.GRAY))));

        MutableComponent hint = Component.literal("Use /tpaccept " + senderName + " or /tpdeny " + senderName)
                .withStyle(ChatFormatting.DARK_GRAY);

        target.sendSystemMessage(Messages.custom(header.append(Component.literal("\n")).append(acceptBtn).append(denyBtn).append(Component.literal("\n")).append(hint)));
    }
}

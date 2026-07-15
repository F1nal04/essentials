package f1nal.essentials.command;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;

import f1nal.essentials.Essentials;
import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.moderation.DurationParser;
import f1nal.essentials.moderation.IpAddressUtil;
import f1nal.essentials.moderation.IpBanRecord;
import f1nal.essentials.moderation.ModerationManager;
import f1nal.essentials.moderation.ModerationMessages;
import f1nal.essentials.moderation.Moderator;
import f1nal.essentials.moderation.PlayerIpBanResult;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class BanIpCommand {

    private BanIpCommand() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess,
            Commands.CommandSelection environment,
            CommandConfig.CommandSettings settings) {
        LiteralCommandNode<CommandSourceStack> banIp = dispatcher.register(
                Commands.literal("ban-ip")
                        .requires(settings.getPermissionRequirement())
                        .then(Commands.argument("address-or-player", IpBanTargetArgument.target())
                                .then(Commands.argument("duration", StringArgumentType.word())
                                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                                                .executes(ctx -> banIp(
                                                        ctx.getSource(),
                                                        IpBanTargetArgument.getTarget(
                                                                ctx, "address-or-player"),
                                                        StringArgumentType.getString(ctx, "duration"),
                                                        StringArgumentType.getString(ctx, "reason")))))));

        dispatcher.register(Commands.literal("banip")
                .requires(settings.getPermissionRequirement())
                .redirect(banIp));
    }

    private static int banIp(
            CommandSourceStack source,
            String addressOrPlayer,
            String durationText,
            String reasonText) {
        String reason = reasonText.trim();
        if (reason.isEmpty()) {
            source.sendFailure(Messages.error("An IP-ban reason is required."));
            return 0;
        }

        long durationMs;
        try {
            durationMs = DurationParser.parseMillis(durationText);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Messages.error(e.getMessage()));
            return 0;
        }

        Target target = resolveTarget(source, addressOrPlayer);
        if (target == null) {
            return 0;
        }

        Moderator moderator = BanCommand.moderator(source);
        IpBanRecord ban;
        try {
            if (target.playerUuid() == null) {
                Optional<IpBanRecord> result = ModerationManager.get().banIp(
                        target.address(), reason, durationMs, moderator);
                if (result.isEmpty()) {
                    source.sendFailure(Messages.error(target.address() + " is already IP-banned."));
                    return 0;
                }
                ban = result.get();
            } else {
                Optional<PlayerIpBanResult> result = ModerationManager.get().banPlayerIp(
                        target.address(), target.playerUuid(), target.playerName(),
                        reason, durationMs, moderator);
                if (result.isEmpty()) {
                    source.sendFailure(Messages.error(
                            target.playerName() + " and " + target.address()
                                    + " already have active bans."));
                    return 0;
                }
                ban = result.get().ipBan();
            }
        } catch (SQLException | IllegalStateException e) {
            Essentials.LOGGER.error("Failed to persist IP ban for {}", target.address(), e);
            source.sendFailure(Messages.error("The IP ban could not be saved; no ban was applied."));
            return 0;
        } catch (IllegalArgumentException e) {
            source.sendFailure(Messages.error(e.getMessage()));
            return 0;
        }
        List<ServerPlayer> connected = List.copyOf(source.getServer().getPlayerList().getPlayers());
        int disconnected = 0;
        for (ServerPlayer player : connected) {
            Optional<String> playerAddress = IpAddressUtil.fromSocketAddress(
                    player.connection.getRemoteAddress());
            if (playerAddress.filter(target.address()::equals).isPresent()) {
                player.connection.disconnect(ModerationMessages.ipBanDisconnect(
                        ban, ModerationManager.get().nowMs()));
                disconnected++;
            }
        }

        int disconnectedCount = disconnected;
        String subject = target.playerName() == null
                ? "IP-banned " + target.address()
                : "Banned " + target.playerName() + " and IP " + target.address();
        source.sendSuccess(() -> Messages.success(
                subject + " for "
                        + DurationParser.formatDuration(durationMs) + ": " + reason
                        + " (disconnected " + disconnectedCount + " player(s))"), true);
        return 1;
    }

    private static Target resolveTarget(CommandSourceStack source, String input) {
        try {
            return new Target(IpAddressUtil.normalizeLiteral(input), null, null);
        } catch (IllegalArgumentException ignored) {
            // Vanilla also accepts the name of a currently connected player.
        }

        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(input);
        if (player == null) {
            source.sendFailure(Messages.error(
                    "Enter a valid IPv4/IPv6 address or the name of an online player."));
            return null;
        }
        Optional<String> address = IpAddressUtil.fromSocketAddress(
                player.connection.getRemoteAddress());
        if (address.isEmpty()) {
            source.sendFailure(Messages.error(
                    "That player does not have a network IP address to ban."));
            return null;
        }
        return new Target(address.get(), player.getUUID(), player.getName().getString());
    }

    private record Target(String address, UUID playerUuid, String playerName) {
    }
}

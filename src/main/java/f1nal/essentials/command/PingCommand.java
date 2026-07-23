package f1nal.essentials.command;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig.CommandSettings;
import f1nal.essentials.config.PingConfig;
import f1nal.essentials.moderation.LegacyTextFormatter;
import f1nal.essentials.ping.PingAccess;
import f1nal.essentials.ping.PingFormatter;
import f1nal.essentials.vanish.VanishManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/** Reports the keep-alive latency already maintained by the server. */
public final class PingCommand {
    private PingCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess, Commands.CommandSelection environment,
            CommandSettings settings) {
        Predicate<CommandSourceStack> othersPermission =
                settings.getPermissionRequirement("ping.others");
        dispatcher.register(Commands.literal("ping")
                .requires(settings.getPermissionRequirement("ping"))
                .executes(context -> self(context.getSource()))
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests(PingCommand::suggestPlayers)
                        .executes(context -> other(
                                context.getSource(),
                                StringArgumentType.getString(context, "player"),
                                othersPermission))));
    }

    private static int self(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (!PingAccess.canUseSelf(player != null)) {
            return failure(source, PingConfig.get().consoleRequiresPlayerFormat);
        }
        return report(source, player, true);
    }

    private static int other(CommandSourceStack source, String playerName,
            Predicate<CommandSourceStack> othersPermission) {
        boolean playerSource = source.getPlayer() != null;
        boolean authorized = PingAccess.canTargetOther(
                playerSource, !playerSource || othersPermission.test(source));
        if (!authorized) return failure(source, PingConfig.get().insufficientAccessFormat);

        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(playerName);
        boolean visible = target != null && VanishManager.canSee(source, target);
        if (!PingAccess.targetAvailable(target != null, visible)) {
            return failure(source, PingConfig.get().unavailablePlayerFormat);
        }
        return report(source, target, source.getPlayer() == target);
    }

    private static int report(CommandSourceStack source, ServerPlayer target, boolean self) {
        if (target.connection == null) {
            return failure(source, PingConfig.get().unavailableLatencyFormat
                    .replace("{player}", target.getName().getString()));
        }
        int latencyMs = target.connection.latency();
        if (latencyMs < 0) {
            return failure(source, PingConfig.get().unavailableLatencyFormat
                    .replace("{player}", target.getName().getString()));
        }

        PingConfig config = PingConfig.get();
        String formatted = PingFormatter.format(
                self ? config.selfFormat : config.otherFormat,
                target.getName().getString(),
                latencyMs,
                config.goodMaxMs,
                config.moderateMaxMs,
                config.numberFormat);
        source.sendSuccess(() -> Messages.custom(LegacyTextFormatter.parse(formatted)), false);
        return 1;
    }

    private static int failure(CommandSourceStack source, String message) {
        source.sendFailure(Messages.custom(LegacyTextFormatter.parse(message)));
        return 0;
    }

    private static CompletableFuture<Suggestions> suggestPlayers(
            CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            String name = player.getName().getString();
            if (name.toLowerCase(Locale.ROOT).startsWith(remaining)
                    && VanishManager.canSee(context.getSource(), player)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }
}

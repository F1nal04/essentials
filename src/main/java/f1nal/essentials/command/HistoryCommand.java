package f1nal.essentials.command;

import java.sql.SQLException;
import java.time.ZoneId;
import java.util.Collection;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;

import f1nal.essentials.Essentials;
import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.moderation.AuditEntryFormatter;
import f1nal.essentials.moderation.AuditFilter;
import f1nal.essentials.moderation.AuditPage;
import f1nal.essentials.moderation.AuditRecord;
import f1nal.essentials.moderation.ModerationManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.players.NameAndId;

public final class HistoryCommand {

    private static final int PAGE_SIZE = 10;
    private static final int MAX_PAGE = 1_000_000;

    private HistoryCommand() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess,
            Commands.CommandSelection environment,
            CommandConfig.CommandSettings settings) {
        LiteralCommandNode<CommandSourceStack> history = dispatcher.register(
                Commands.literal("history")
                        .requires(settings.getPermissionRequirement())
                        .then(Commands.argument("player", GameProfileArgument.gameProfile())
                                .executes(ctx -> history(
                                        ctx.getSource(),
                                        GameProfileArgument.getGameProfiles(ctx, "player"),
                                        AuditFilter.ALL,
                                        1))
                                .then(Commands.argument("filter", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("all");
                                            builder.suggest("bans");
                                            builder.suggest("kicks");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> history(
                                                ctx.getSource(),
                                                GameProfileArgument.getGameProfiles(ctx, "player"),
                                                parseFilter(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "filter")),
                                                1))
                                        .then(Commands.argument(
                                                        "page",
                                                        IntegerArgumentType.integer(1, MAX_PAGE))
                                                .executes(ctx -> history(
                                                        ctx.getSource(),
                                                        GameProfileArgument.getGameProfiles(ctx, "player"),
                                                        parseFilter(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "filter")),
                                                        IntegerArgumentType.getInteger(ctx, "page")))))));

        dispatcher.register(Commands.literal("audit")
                .requires(settings.getPermissionRequirement())
                .redirect(history));
    }

    private static AuditFilter parseFilter(CommandSourceStack source, String input) {
        try {
            return AuditFilter.parse(input);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Messages.error(e.getMessage()));
            return null;
        }
    }

    private static int history(
            CommandSourceStack source,
            Collection<NameAndId> targets,
            AuditFilter filter,
            int pageNumber) {
        if (filter == null) {
            return 0;
        }
        if (targets.size() != 1) {
            source.sendFailure(Messages.error("Please specify exactly one player."));
            return 0;
        }

        NameAndId target = targets.iterator().next();
        int offset = (pageNumber - 1) * PAGE_SIZE;
        AuditPage page;
        try {
            page = ModerationManager.get().history(target.id(), filter, PAGE_SIZE, offset);
        } catch (SQLException | IllegalStateException e) {
            Essentials.LOGGER.error("Failed to read moderation history for {}", target.id(), e);
            source.sendFailure(Messages.error("The moderation history could not be loaded."));
            return 0;
        }

        long totalPages = page.totalRecords() == 0
                ? 1
                : ((page.totalRecords() - 1) / PAGE_SIZE) + 1;
        if (pageNumber > totalPages) {
            source.sendFailure(Messages.error(
                    "Page " + pageNumber + " does not exist; the last page is " + totalPages + "."));
            return 0;
        }

        source.sendSuccess(() -> Messages.info(
                "Moderation history for " + target.name()
                        + " — " + filter.argumentValue()
                        + " — page " + pageNumber + "/" + totalPages
                        + " (" + page.totalRecords() + " records)"), false);
        if (page.records().isEmpty()) {
            source.sendSuccess(() -> Messages.info("No matching moderation records."), false);
            return 1;
        }

        ZoneId serverZone = ZoneId.systemDefault();
        for (AuditRecord record : page.records()) {
            source.sendSuccess(
                    () -> Messages.info(AuditEntryFormatter.format(record, serverZone)), false);
        }
        return page.records().size();
    }
}

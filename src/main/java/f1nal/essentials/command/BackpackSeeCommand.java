package f1nal.essentials.command;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import f1nal.essentials.Messages;
import f1nal.essentials.backpack.BackpackManager;
import f1nal.essentials.config.BackpackConfig;
import f1nal.essentials.config.CommandConfig;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

/** Operator view for another player's per-player backpack. */
public final class BackpackSeeCommand {

    private static final Map<UUID, ViewSession> BY_VIEWER = new HashMap<>();

    private BackpackSeeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess, Commands.CommandSelection environment,
            CommandConfig.CommandSettings settings) {
        LiteralCommandNode<CommandSourceStack> backpackSee = dispatcher.register(
                command("backpacksee").requires(settings.getPermissionRequirement()));

        dispatcher.register(Commands.literal("bpsee")
                .requires(settings.getPermissionRequirement())
                .redirect(backpackSee));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> command(String name) {
        return Commands.literal(name)
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(ctx -> open(ctx.getSource(),
                                GameProfileArgument.getGameProfiles(ctx, "player"))));
    }

    private static int open(CommandSourceStack source, Collection<NameAndId> targets) {
        ServerPlayer viewer = source.getPlayer();
        if (viewer == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }
        if (BackpackConfig.get().mode != BackpackConfig.Mode.PER_PLAYER) {
            source.sendFailure(Messages.error("Backpack lookup is only available in per-player mode."));
            return 0;
        }
        if (targets.size() != 1) {
            source.sendFailure(Messages.error("Please specify exactly one player."));
            return 0;
        }

        NameAndId target = targets.iterator().next();
        if (source.getServer().getPlayerList().getPlayer(target.id()) == null
                && source.getServer().getPlayerList().loadPlayerData(target).isEmpty()) {
            source.sendFailure(Messages.error(target.name() + " has never joined this server."));
            return 0;
        }

        SimpleContainer backpack = BackpackManager.getOrCreateBackpack(target.id(), source.getServer());
        ViewSession session = new ViewSession(viewer.getUUID(), target.id(), backpack, source.getServer());
        BY_VIEWER.put(viewer.getUUID(), session);

        viewer.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, player) -> new ChestMenu(
                        MenuType.GENERIC_9x3, syncId, playerInventory, backpack, 3) {
                    @Override
                    public void removed(Player player) {
                        super.removed(player);
                        session.finish();
                    }
                },
                Component.literal(target.name() + "'s Backpack")
        ));
        source.sendSuccess(() -> Messages.info("Viewing " + target.name() + "'s backpack."), false);
        return 1;
    }

    public static void finishForViewer(UUID viewerId) {
        ViewSession session = BY_VIEWER.get(viewerId);
        if (session != null) {
            session.finish();
        }
    }

    public static void finishAll() {
        for (ViewSession session : java.util.List.copyOf(BY_VIEWER.values())) {
            session.finish();
        }
    }

    public static boolean isTargetBeingViewed(UUID targetId) {
        return BY_VIEWER.values().stream().anyMatch(session -> session.targetId.equals(targetId));
    }

    private static final class ViewSession {

        private final UUID viewerId;
        private final UUID targetId;
        private final SimpleContainer backpack;
        private final MinecraftServer server;
        private boolean finished;

        private ViewSession(UUID viewerId, UUID targetId, SimpleContainer backpack, MinecraftServer server) {
            this.viewerId = viewerId;
            this.targetId = targetId;
            this.backpack = backpack;
            this.server = server;
        }

        private void finish() {
            if (finished) {
                return;
            }
            finished = true;
            BY_VIEWER.remove(viewerId, this);
            BackpackManager.saveBackpack(targetId, backpack, server);
            if (server.getPlayerList().getPlayer(targetId) == null && !isTargetBeingViewed(targetId)) {
                BackpackManager.saveAndUnloadPlayer(targetId, server);
            }
        }
    }
}

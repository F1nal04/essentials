package f1nal.essentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

public final class EnderChestSeeCommand {

    private EnderChestSeeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment, CommandConfig.CommandSettings settings) {
        LiteralCommandNode<CommandSourceStack> enderChestSee = dispatcher.register(
                command("enderchestsee").requires(settings.getPermissionRequirement("enderchestsee")));

        dispatcher.register(Commands.literal("esee")
                .requires(settings.getPermissionRequirement("esee"))
                .redirect(enderChestSee));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> command(String name) {
        return Commands.literal(name)
                .then(Commands.argument("player", GameProfileArgument.gameProfile())
                        .executes(ctx -> open(ctx.getSource(),
                                GameProfileArgument.getGameProfiles(ctx, "player"))));
    }

    private static int open(CommandSourceStack source,
            java.util.Collection<net.minecraft.server.players.NameAndId> targets) {
        ServerPlayer viewer = source.getPlayer();
        if (viewer == null) {
            source.sendFailure(Messages.error("You must be a player to use this command."));
            return 0;
        }

        if (targets.size() != 1) {
            source.sendFailure(Messages.error("Please specify exactly one player."));
            return 0;
        }

        net.minecraft.server.players.NameAndId profile = targets.iterator().next();
        ServerPlayer target = source.getServer().getPlayerList().getPlayer(profile.id());
        if (target != null) {
            return openOnline(source, viewer, target);
        }

        OfflinePlayerDataManager.AcquireResult result = OfflinePlayerDataManager.acquire(
                source.getServer(), profile, viewer.getUUID());
        if (result.status() == OfflinePlayerDataManager.AcquireStatus.NOT_FOUND) {
            source.sendFailure(Messages.error(profile.name() + " has never joined this server."));
            return 0;
        }
        if (result.status() == OfflinePlayerDataManager.AcquireStatus.BUSY) {
            source.sendFailure(Messages.error("That player's data is already being edited."));
            return 0;
        }

        OfflinePlayerDataManager.Session session = result.session();
        viewer.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, player) -> new OfflineEnderChestMenu(
                        syncId, playerInventory, session),
                Component.literal(profile.name() + "'s Ender Chest")
        ));
        source.sendSuccess(() -> Messages.info("Viewing " + profile.name() + "'s ender chest."), false);
        return 1;
    }

    private static int openOnline(CommandSourceStack source, ServerPlayer viewer, ServerPlayer target) {
        String name = target.getName().getString();
        viewer.openMenu(new SimpleMenuProvider(
                (syncId, playerInventory, player) -> ChestMenu.threeRows(syncId, playerInventory, target.getEnderChestInventory()),
                Component.literal(name + "'s Ender Chest")
        ));

        source.sendSuccess(() -> Messages.info("Viewing " + name + "'s ender chest."), false);
        return 1;
    }

    private static final class OfflineEnderChestMenu extends ChestMenu {

        private final OfflinePlayerDataManager.Session session;

        private OfflineEnderChestMenu(int syncId, net.minecraft.world.entity.player.Inventory playerInventory,
                OfflinePlayerDataManager.Session session) {
            super(MenuType.GENERIC_9x3, syncId, playerInventory, session.enderChest(), 3);
            this.session = session;
        }

        @Override
        public boolean stillValid(net.minecraft.world.entity.player.Player player) {
            return session.isStillOffline();
        }

        @Override
        public void removed(net.minecraft.world.entity.player.Player player) {
            super.removed(player);
            session.finish();
        }
    }
}

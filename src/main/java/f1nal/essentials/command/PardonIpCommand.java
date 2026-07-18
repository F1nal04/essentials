package f1nal.essentials.command;

import java.sql.SQLException;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;

import f1nal.essentials.Essentials;
import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.moderation.IpAddressUtil;
import f1nal.essentials.moderation.ModerationManager;
import f1nal.essentials.moderation.Moderator;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class PardonIpCommand {

    private PardonIpCommand() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess,
            Commands.CommandSelection environment,
            CommandConfig.CommandSettings settings) {
        LiteralCommandNode<CommandSourceStack> pardonIp = dispatcher.register(
                Commands.literal("pardon-ip")
                        .requires(settings.getPermissionRequirement("pardon-ip"))
                        .then(Commands.argument("address", StringArgumentType.string())
                                .executes(ctx -> pardonIp(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "address")))));

        dispatcher.register(Commands.literal("unban-ip")
                .requires(settings.getPermissionRequirement("unban-ip"))
                .redirect(pardonIp));
    }

    private static int pardonIp(CommandSourceStack source, String addressText) {
        String address;
        try {
            address = IpAddressUtil.normalizeLiteral(addressText);
        } catch (IllegalArgumentException e) {
            source.sendFailure(Messages.error(e.getMessage()));
            return 0;
        }

        Moderator moderator = BanCommand.moderator(source);
        boolean revoked;
        try {
            revoked = ModerationManager.get().pardonIp(address, moderator);
        } catch (SQLException | IllegalStateException e) {
            Essentials.LOGGER.error("Failed to revoke IP ban for {}", address, e);
            source.sendFailure(Messages.error(
                    "The IP ban could not be revoked; no change was made."));
            return 0;
        }

        if (!revoked) {
            source.sendFailure(Messages.error(address + " is not currently IP-banned."));
            return 0;
        }

        source.sendSuccess(() -> Messages.success("Unbanned IP " + address + "."), true);
        return 1;
    }
}

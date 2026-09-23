package f1nal.essentials.command;

import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import f1nal.essentials.Messages;
import f1nal.essentials.config.CommandConfig;
import f1nal.essentials.config.HomeConfig;
import f1nal.essentials.home.HomeManager;
import f1nal.essentials.home.HomeMessages;
import f1nal.essentials.home.HomeText;
import f1nal.essentials.moderation.LegacyTextFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public final class HomesCommand {
    private HomesCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext registryAccess, Commands.CommandSelection environment,
            CommandConfig.CommandSettings settings) {
        dispatcher.register(Commands.literal("homes")
                .requires(settings.getPermissionRequirement("homes"))
                .executes(HomesCommand::listHomes));
    }

    private static int listHomes(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        HomeConfig config = HomeConfig.get();
        if (player == null) {
            source.sendFailure(HomeMessages.format(config.playerOnlyMessage));
            return 0;
        }
        List<String> names = HomeManager.names(player.getUUID());
        if (names.isEmpty()) {
            source.sendSuccess(() -> HomeMessages.format(
                    config.noHomesMessage, null, null, null, 0), false);
            return 1;
        }
        String header = HomeText.fill(config.listMessage, null, null, null, names.size());
        MutableComponent body = Component.empty().append(LegacyTextFormatter.parse(header));
        for (int index = 0; index < names.size(); index++) {
            if (index > 0) {
                body.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            }
            String name = names.get(index);
            String command = "/home " + name;
            body.append(Component.literal(name).withStyle(style -> style
                    .withColor(ChatFormatting.LIGHT_PURPLE)
                    .withUnderlined(true)
                    .withClickEvent(new ClickEvent.SuggestCommand(command))
                    .withHoverEvent(new HoverEvent.ShowText(Component.literal(
                            "Suggest " + command).withStyle(ChatFormatting.GRAY)))));
        }
        source.sendSuccess(() -> Messages.custom(body), false);
        return 1;
    }
}

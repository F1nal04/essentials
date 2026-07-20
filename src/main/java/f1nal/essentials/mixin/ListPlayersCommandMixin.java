package f1nal.essentials.mixin;

import java.util.List;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import f1nal.essentials.vanish.VanishManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.commands.ListPlayersCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

/** Makes vanilla /list viewer-aware and marks vanished players for authorized staff. */
@Mixin(ListPlayersCommand.class)
abstract class ListPlayersCommandMixin {
    @Inject(method = "format", at = @At("HEAD"), cancellable = true)
    private static void essentials$visiblePlayers(CommandSourceStack source,
            Function<ServerPlayer, Component> formatter,
            CallbackInfoReturnable<Integer> callback) {
        PlayerList playerList = source.getServer().getPlayerList();
        List<ServerPlayer> visible = playerList.getPlayers().stream()
                .filter(player -> VanishManager.canSee(source, player)).toList();
        Component names = ComponentUtils.formatList(visible, player -> {
            Component name = formatter.apply(player);
            return VanishManager.isVanished(player.getUUID())
                    ? name.copy().append(Component.literal(" [V]").withStyle(ChatFormatting.GRAY))
                    : name;
        });
        source.sendSuccess(() -> Component.translatable("commands.list.players",
                visible.size(), playerList.getMaxPlayers(), names), false);
        callback.setReturnValue(visible.size());
    }
}

package f1nal.essentials.vanish;

import f1nal.essentials.config.VanishConfig;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;

/** Prevents public chat from revealing a vanished sender. */
public final class VanishChatEnforcement {
    private VanishChatEnforcement() {
    }

    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(
                (message, sender, boundChatType) -> allow(message, sender));
        ServerMessageEvents.ALLOW_COMMAND_MESSAGE.register((message, source, boundChatType) ->
                !(source.getEntity() instanceof ServerPlayer player) || allow(message, player));
    }

    private static boolean allow(PlayerChatMessage message, ServerPlayer sender) {
        if (!VanishManager.isVanished(sender.getUUID())) return true;
        VanishConfig config = VanishConfig.get();
        if (config.chatBehavior == VanishConfig.ChatBehavior.STAFF_ONLY) {
            var rendered = VanishManager.format(config.staffChatFormat,
                    sender.getName().getString(), message.decoratedContent().getString());
            VanishManager.broadcastStaffMessage(rendered, sender);
        }
        sender.sendSystemMessage(VanishManager.format(config.chatBlockedMessage,
                sender.getName().getString(), message.decoratedContent().getString()));
        return false;
    }
}

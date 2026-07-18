package f1nal.essentials.moderation;

import f1nal.essentials.config.ModerationConfig;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/** Blocks communication from muted players using the in-memory active-mute cache. */
public final class MuteEnforcement {
    private MuteEnforcement() {
    }

    public static void register() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(
                (message, sender, boundChatType) -> allow(sender));
        ServerMessageEvents.ALLOW_COMMAND_MESSAGE.register(
                (message, source, boundChatType) -> allow(source));
    }

    public static boolean allowPrivateMessage(CommandSourceStack source) {
        return !ModerationConfig.get().muteBlocksPrivateMessages || allow(source);
    }

    private static boolean allow(CommandSourceStack source) {
        return source.getEntity() instanceof ServerPlayer player && allow(player)
                || !(source.getEntity() instanceof ServerPlayer);
    }

    private static boolean allow(ServerPlayer player) {
        var mute = ModerationManager.activeMute(player.getUUID());
        if (mute.isEmpty()) {
            ModerationManager.get().consumeExpiredMuteNotification(player.getUUID())
                    .ifPresent(expired -> player.sendSystemMessage(
                            ModerationMessages.muteExpired(expired.targetName())));
            return true;
        }
        player.sendSystemMessage(ModerationMessages.muteBlocked(
                mute.get(), ModerationManager.get().nowMs()));
        return false;
    }
}

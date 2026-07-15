package f1nal.essentials.moderation;

import f1nal.essentials.config.ModerationConfig;
import net.minecraft.network.chat.Component;

public final class ModerationMessages {

    private ModerationMessages() {
    }

    public static Component banDisconnect(BanRecord ban, long nowMs) {
        String message = ModerationMessageFormatter.banMessage(
                ModerationConfig.get().banMessage, ban, nowMs);
        return LegacyTextFormatter.parse(message);
    }

    public static Component kickDisconnect(String targetName, String reason, Moderator moderator) {
        String message = ModerationMessageFormatter.kickMessage(
                ModerationConfig.get().kickMessage, targetName, reason, moderator.name());
        return LegacyTextFormatter.parse(message);
    }
}

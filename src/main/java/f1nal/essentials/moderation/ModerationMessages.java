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

    public static Component ipBanDisconnect(IpBanRecord ban, long nowMs) {
        String message = ModerationMessageFormatter.ipBanMessage(
                ModerationConfig.get().banMessage, ban, nowMs);
        return LegacyTextFormatter.parse(message);
    }

    public static Component kickDisconnect(String targetName, String reason, Moderator moderator) {
        String message = ModerationMessageFormatter.kickMessage(
                ModerationConfig.get().kickMessage, targetName, reason, moderator.name());
        return LegacyTextFormatter.parse(message);
    }

    public static Component warning(WarningRecord warning) {
        return LegacyTextFormatter.parse(replaceCommon(
                ModerationConfig.get().warningMessage,
                warning.targetName(), warning.reason(), warning.moderatorName(), null));
    }

    public static Component muted(MuteRecord mute, long nowMs) {
        return LegacyTextFormatter.parse(replaceCommon(
                ModerationConfig.get().muteMessage,
                mute.targetName(), mute.reason(), mute.moderatorName(), remaining(mute, nowMs)));
    }

    public static Component muteBlocked(MuteRecord mute, long nowMs) {
        return LegacyTextFormatter.parse(replaceCommon(
                ModerationConfig.get().muteBlockedMessage,
                mute.targetName(), mute.reason(), mute.moderatorName(), remaining(mute, nowMs)));
    }

    public static Component unmuted(String targetName, Moderator moderator) {
        return LegacyTextFormatter.parse(replaceCommon(
                ModerationConfig.get().unmuteMessage,
                targetName, "", moderator.name(), null));
    }

    public static Component muteExpired(String targetName) {
        return LegacyTextFormatter.parse(replaceCommon(
                ModerationConfig.get().muteExpiredMessage,
                targetName, "", "", null));
    }

    private static String remaining(MuteRecord mute, long nowMs) {
        return mute.permanent()
                ? "Permanent"
                : DurationParser.formatRemaining(mute.expiresAtMs() - nowMs);
    }

    private static String replaceCommon(
            String template, String player, String reason, String moderator, String time) {
        String result = template
                .replace("{player}", player)
                .replace("{reason}", reason)
                .replace("{moderator}", moderator);
        return time == null ? result : result.replace("{time}", time);
    }
}

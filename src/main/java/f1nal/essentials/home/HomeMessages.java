package f1nal.essentials.home;

import f1nal.essentials.Messages;
import f1nal.essentials.moderation.LegacyTextFormatter;
import net.minecraft.network.chat.Component;

/** Placeholder substitution and legacy formatting for configured home text. */
public final class HomeMessages {
    private HomeMessages() {
    }

    public static Component format(String template) {
        return Messages.custom(LegacyTextFormatter.parse(template));
    }

    public static Component format(String template, String name, Long seconds, Integer limit,
            Integer count) {
        return format(HomeText.fill(template, name, seconds, limit, count));
    }
}

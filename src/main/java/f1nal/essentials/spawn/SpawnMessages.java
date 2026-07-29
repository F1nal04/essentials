package f1nal.essentials.spawn;

import f1nal.essentials.Messages;
import f1nal.essentials.moderation.LegacyTextFormatter;
import net.minecraft.network.chat.Component;

/** Placeholder substitution and legacy formatting for configured spawn text. */
public final class SpawnMessages {
    private SpawnMessages() {
    }

    public static Component format(String template) {
        return Messages.custom(LegacyTextFormatter.parse(template));
    }

    public static Component format(String template, long seconds) {
        return format(template.replace("{seconds}", Long.toString(seconds)));
    }
}

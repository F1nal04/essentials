package f1nal.essentials.messaging;

import f1nal.essentials.moderation.LegacyTextFormatter;
import net.minecraft.network.chat.Component;

public final class MessageFormatter {
    private MessageFormatter() {
    }

    public static Component format(String template, String sender, String recipient, String message) {
        return LegacyTextFormatter.parse(template
                .replace("{sender}", sender)
                .replace("{recipient}", recipient)
                .replace("{message}", message));
    }
}

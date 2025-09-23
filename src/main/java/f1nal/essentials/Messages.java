package f1nal.essentials;

import f1nal.essentials.config.TagConfig;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class Messages {

    private static final TagConfig TAG = TagConfig.loadOrDefaults();

    private Messages() {
    }

    public static MutableText prefix() {
        if (TAG.bold) {
            return Text.literal("[")
                    .formatted(TAG.bracketColor)
                    .append(Text.literal(TAG.text).formatted(TAG.color, Formatting.BOLD))
                    .append(Text.literal("] ").formatted(TAG.bracketColor));
        }
        return Text.literal("[")
                .formatted(TAG.bracketColor)
                .append(Text.literal(TAG.text).formatted(TAG.color))
                .append(Text.literal("] ").formatted(TAG.bracketColor));
    }

    public static Text info(String message) {
        return prefix().append(Text.literal(message).formatted(Formatting.GRAY));
    }

    public static Text success(String message) {
        return prefix().append(Text.literal(message).formatted(Formatting.GREEN));
    }

    public static Text warning(String message) {
        return prefix().append(Text.literal(message).formatted(Formatting.YELLOW));
    }

    public static Text error(String message) {
        return prefix().append(Text.literal(message).formatted(Formatting.RED));
    }

    public static Text custom(Text message) {
        return prefix().append(message);
    }

    public static Text raw(String message, Formatting... formats) {
        MutableText body = Text.literal(message);
        if (formats != null) {
            for (Formatting fmt : formats) {
                if (fmt != null) {
                    body.formatted(fmt);
                }
            }
        }
        return prefix().append(body);
    }
}

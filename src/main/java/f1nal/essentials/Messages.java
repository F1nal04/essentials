package f1nal.essentials;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class Messages {

    private static final String TAG = "Essentials";

    private Messages() {
    }

    public static MutableText prefix() {
        return Text.literal("[")
                .formatted(Formatting.DARK_GRAY)
                .append(Text.literal(TAG).formatted(Formatting.AQUA, Formatting.BOLD))
                .append(Text.literal("] ").formatted(Formatting.DARK_GRAY));
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

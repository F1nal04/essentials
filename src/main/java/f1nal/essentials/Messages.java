package f1nal.essentials;

import f1nal.essentials.config.TagConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class Messages {

    private static final TagConfig TAG = TagConfig.loadOrDefaults();

    private Messages() {
    }

    public static MutableComponent prefix() {
        if (TAG.bold) {
            return Component.literal("[")
                    .withStyle(TAG.bracketColor)
                    .append(Component.literal(TAG.text).withStyle(TAG.color, ChatFormatting.BOLD))
                    .append(Component.literal("] ").withStyle(TAG.bracketColor));
        }
        return Component.literal("[")
                .withStyle(TAG.bracketColor)
                .append(Component.literal(TAG.text).withStyle(TAG.color))
                .append(Component.literal("] ").withStyle(TAG.bracketColor));
    }

    public static Component info(String message) {
        return prefix().append(Component.literal(message).withStyle(ChatFormatting.GRAY));
    }

    public static Component success(String message) {
        return prefix().append(Component.literal(message).withStyle(ChatFormatting.GREEN));
    }

    public static Component warning(String message) {
        return prefix().append(Component.literal(message).withStyle(ChatFormatting.YELLOW));
    }

    public static Component error(String message) {
        return prefix().append(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    public static Component custom(Component message) {
        return prefix().append(message);
    }

    public static Component raw(String message, ChatFormatting... formats) {
        MutableComponent body = Component.literal(message);
        if (formats != null) {
            for (ChatFormatting fmt : formats) {
                if (fmt != null) {
                    body.withStyle(fmt);
                }
            }
        }
        return prefix().append(body);
    }
}

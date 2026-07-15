package f1nal.essentials.moderation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Parses Minecraft's familiar ampersand formatting codes in configured text. */
public final class LegacyTextFormatter {

    private LegacyTextFormatter() {
    }

    public static Component parse(String text) {
        MutableComponent result = Component.empty();
        StringBuilder segment = new StringBuilder();
        Style style = Style.EMPTY;

        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '&' && i + 1 < text.length()) {
                ChatFormatting formatting = ChatFormatting.getByCode(text.charAt(i + 1));
                if (formatting != null) {
                    append(result, segment, style);
                    style = style.applyLegacyFormat(formatting);
                    i++;
                    continue;
                }
                if (text.charAt(i + 1) == '&') {
                    segment.append('&');
                    i++;
                    continue;
                }
            }
            segment.append(current);
        }
        append(result, segment, style);
        return result;
    }

    private static void append(MutableComponent result, StringBuilder segment, Style style) {
        if (!segment.isEmpty()) {
            result.append(Component.literal(segment.toString()).withStyle(style));
            segment.setLength(0);
        }
    }
}

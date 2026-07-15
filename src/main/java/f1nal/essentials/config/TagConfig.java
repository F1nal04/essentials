package f1nal.essentials.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import net.minecraft.ChatFormatting;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

public final class TagConfig {

    public final String text;
    public final ChatFormatting color;
    public final ChatFormatting bracketColor;
    public final boolean bold;

    private TagConfig(String text, ChatFormatting color, ChatFormatting bracketColor, boolean bold) {
        this.text = text;
        this.color = color;
        this.bracketColor = bracketColor;
        this.bold = bold;
    }

    public static TagConfig loadOrDefaults() {
        Path cfg = ConfigPaths.configFile();
        if (!Files.exists(cfg)) {
            return defaults();
        }
        try {
            return parse(Files.readString(cfg, StandardCharsets.UTF_8));
        } catch (Exception e) {
            f1nal.essentials.Essentials.LOGGER.warn("Failed to read tag settings from essentials.yaml, using defaults: {}", e.toString());
            return defaults();
        }
    }

    static TagConfig parse(String yamlText) {
        try {
            Object root = new Yaml(new LoaderOptions()).load(yamlText);
            if (!(root instanceof Map<?, ?> map)) {
                return defaults();
            }
            Object tagObj = map.get("tag");
            if (!(tagObj instanceof Map<?, ?> tag)) {
                return defaults();
            }
            String text = coerceString(tag.get("text"), null);
            String colorStr = coerceString(tag.get("color"), null);
            String bracketColorStr = coerceString(tag.get("bracketColor"), null);
            Boolean boldObj = tag.get("bold") instanceof Boolean ? (Boolean) tag.get("bold") : null;

            if (text == null || colorStr == null || bracketColorStr == null || boldObj == null) {
                return defaults();
            }

            ChatFormatting color = parseFormatting(colorStr, null);
            ChatFormatting bracketColor = parseFormatting(bracketColorStr, null);

            if (color == null || bracketColor == null) {
                return defaults();
            }

            return new TagConfig(text, color, bracketColor, boldObj);
        } catch (Exception e) {
            f1nal.essentials.Essentials.LOGGER.warn("Failed to read tag settings from essentials.yaml, using defaults: {}", e.toString());
            return defaults();
        }
    }

    private static TagConfig defaults() {
        return new TagConfig("Essentials", ChatFormatting.DARK_PURPLE, ChatFormatting.DARK_GRAY, true);
    }

    private static String coerceString(Object value, String def) {
        return value instanceof String s ? s : def;
    }

    private static ChatFormatting parseFormatting(String name, ChatFormatting def) {
        if (name == null || name.trim().isEmpty()) return def;
        try {
            return ChatFormatting.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}


package f1nal.essentials.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Formatting;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

public final class TagSettings {

    public final String text;
    public final Formatting color;
    public final Formatting bracketColor;
    public final boolean bold;

    private TagSettings(String text, Formatting color, Formatting bracketColor, boolean bold) {
        this.text = text;
        this.color = color;
        this.bracketColor = bracketColor;
        this.bold = bold;
    }

    public static TagSettings loadOrDefaults() {
        Path cfg = FabricLoader.getInstance().getConfigDir().resolve("essentials.yaml");
        if (!Files.exists(cfg)) {
            return defaults();
        }
        try (Reader reader = Files.newBufferedReader(cfg, StandardCharsets.UTF_8)) {
            Yaml yaml = new Yaml(new LoaderOptions());
            Object root = yaml.load(reader);
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

            Formatting color = parseFormatting(colorStr, null);
            Formatting bracketColor = parseFormatting(bracketColorStr, null);

            if (color == null || bracketColor == null) {
                return defaults();
            }

            return new TagSettings(text, color, bracketColor, boldObj);
        } catch (IOException e) {
            return defaults();
        }
    }

    private static TagSettings defaults() {
        return new TagSettings("Essentials", Formatting.AQUA, Formatting.DARK_GRAY, true);
    }

    private static String coerceString(Object value, String def) {
        return value instanceof String s ? s : def;
    }

    private static boolean coerceBoolean(Object value, boolean def) {
        if (value instanceof Boolean b) return b;
        if (value instanceof String s) {
            String n = s.trim().toLowerCase(Locale.ROOT);
            if (n.equals("true") || n.equals("false")) {
                return Boolean.parseBoolean(n);
            }
        }
        return def;
    }

    private static Formatting parseFormatting(String name, Formatting def) {
        if (name == null || name.trim().isEmpty()) return def;
        try {
            return Formatting.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}



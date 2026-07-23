package f1nal.essentials.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import f1nal.essentials.Essentials;
import f1nal.essentials.tps.TpsDisplay.Health;
import net.minecraft.ChatFormatting;

public final class TpsConfig {

    public final Band healthy;
    public final Band degraded;
    public final Band critical;

    private TpsConfig(Band healthy, Band degraded, Band critical) {
        this.healthy = healthy;
        this.degraded = degraded;
        this.critical = critical;
    }

    public static TpsConfig loadOrDefaults() {
        Path cfg = ConfigPaths.configFile();
        if (!Files.exists(cfg)) {
            return defaults();
        }
        try {
            return parse(Files.readString(cfg, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Essentials.LOGGER.warn(
                    "Failed to read TPS settings from essentials.yaml, using defaults: {}", e.toString());
            return defaults();
        }
    }

    static TpsConfig parse(String yamlText) {
        try {
            Object root = new Yaml(new LoaderOptions()).load(yamlText);
            if (!(root instanceof Map<?, ?> rootMap)
                    || !(rootMap.get("tps") instanceof Map<?, ?> tps)) {
                return defaults();
            }

            Band healthy = parseBand(tps.get("healthy"));
            Band degraded = parseBand(tps.get("degraded"));
            Band critical = parseBand(tps.get("critical"));
            if (healthy == null || degraded == null || critical == null
                    || healthy.minimumTps < degraded.minimumTps
                    || degraded.minimumTps < critical.minimumTps) {
                return defaults();
            }
            return new TpsConfig(healthy, degraded, critical);
        } catch (Exception e) {
            Essentials.LOGGER.warn(
                    "Failed to parse TPS settings from essentials.yaml, using defaults: {}", e.toString());
            return defaults();
        }
    }

    public ChatFormatting color(Health health) {
        return switch (health) {
            case HEALTHY -> healthy.color;
            case DEGRADED -> degraded.color;
            case CRITICAL -> critical.color;
        };
    }

    private static Band parseBand(Object value) {
        if (!(value instanceof Map<?, ?> map)
                || !(map.get("minimum_tps") instanceof Number number)
                || !(map.get("color") instanceof String colorName)) {
            return null;
        }
        double minimumTps = number.doubleValue();
        if (!Double.isFinite(minimumTps) || minimumTps < 0) {
            return null;
        }
        try {
            ChatFormatting color = ChatFormatting.valueOf(
                    colorName.trim().toUpperCase(Locale.ROOT));
            if (color == ChatFormatting.OBFUSCATED
                    || color == ChatFormatting.BOLD
                    || color == ChatFormatting.STRIKETHROUGH
                    || color == ChatFormatting.UNDERLINE
                    || color == ChatFormatting.ITALIC
                    || color == ChatFormatting.RESET) {
                return null;
            }
            return new Band(minimumTps, color);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static TpsConfig defaults() {
        return new TpsConfig(
                new Band(18.0, ChatFormatting.GREEN),
                new Band(15.0, ChatFormatting.YELLOW),
                new Band(0.0, ChatFormatting.RED));
    }

    public record Band(double minimumTps, ChatFormatting color) {
    }
}

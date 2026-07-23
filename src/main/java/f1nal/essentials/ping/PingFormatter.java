package f1nal.essentials.ping;

import java.util.Locale;

import f1nal.essentials.config.PingConfig.NumberFormat;

/** Pure latency classification and template formatting. */
public final class PingFormatter {
    public enum Quality {
        GOOD("&a", "good"),
        MODERATE("&e", "moderate"),
        POOR("&c", "poor");

        private final String colorCode;
        private final String displayName;

        Quality(String colorCode, String displayName) {
            this.colorCode = colorCode;
            this.displayName = displayName;
        }
    }

    private PingFormatter() {
    }

    public static Quality quality(int latencyMs, int goodMaxMs, int moderateMaxMs) {
        if (latencyMs <= goodMaxMs) return Quality.GOOD;
        if (latencyMs <= moderateMaxMs) return Quality.MODERATE;
        return Quality.POOR;
    }

    public static String format(String template, String player, int latencyMs,
            int goodMaxMs, int moderateMaxMs, NumberFormat numberFormat) {
        Quality quality = quality(latencyMs, goodMaxMs, moderateMaxMs);
        String latency = numberFormat == NumberFormat.DECIMAL
                ? String.format(Locale.ROOT, "%.1f", (double) latencyMs)
                : Integer.toString(latencyMs);
        return template
                .replace("{player}", player)
                .replace("{latency}", latency)
                .replace("{quality}", quality.displayName)
                .replace("{color}", quality.colorCode);
    }
}

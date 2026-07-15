package f1nal.essentials.moderation;

import java.util.Locale;

public enum AuditFilter {
    ALL("all"),
    BANS("bans"),
    KICKS("kicks");

    private final String argumentValue;

    AuditFilter(String argumentValue) {
        this.argumentValue = argumentValue;
    }

    public String argumentValue() {
        return argumentValue;
    }

    public static AuditFilter parse(String value) {
        if (value == null) {
            throw invalid(value);
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "all" -> ALL;
            case "bans" -> BANS;
            case "kicks" -> KICKS;
            default -> throw invalid(value);
        };
    }

    private static IllegalArgumentException invalid(String value) {
        return new IllegalArgumentException(
                "Unknown history filter '" + value + "'. Use all, bans, or kicks.");
    }
}

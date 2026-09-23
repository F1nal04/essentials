package f1nal.essentials.home;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Case-insensitive home names and configurable name rules. */
public final class HomeNames {
    public static final String FALLBACK_NAME = "home";
    public static final String DEFAULT_PATTERN = "^[A-Za-z0-9_\\-]{1,32}$";

    private HomeNames() {
    }

    public enum Validity { OK, BLANK, INVALID }

    public static Pattern compileOrDefault(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return Pattern.compile(DEFAULT_PATTERN);
        }
        try {
            return Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            return Pattern.compile(DEFAULT_PATTERN);
        }
    }

    public static String canonical(String raw) {
        if (raw == null) return "";
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static Validity validate(String raw, Pattern pattern) {
        if (raw == null || raw.isBlank()) return Validity.BLANK;
        if (pattern == null || !pattern.matcher(raw.trim()).matches()) return Validity.INVALID;
        return Validity.OK;
    }
}

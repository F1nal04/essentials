package f1nal.essentials.moderation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {

    private static final Pattern PART = Pattern.compile("(\\d+)([smhdw])", Pattern.CASE_INSENSITIVE);

    private DurationParser() {
    }

    public static BanDuration parseBanDuration(String input) {
        if (input != null) {
            String normalized = input.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("permanent") || normalized.equals("perm")) {
                return BanDuration.permanentBan();
            }
        }
        return BanDuration.timed(parseMillis(input));
    }

    /** Parses values such as {@code 30m}, {@code 2h}, or {@code 1d12h}. */
    public static long parseMillis(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Duration is required");
        }

        String normalized = input.trim().toLowerCase(Locale.ROOT);
        Matcher matcher = PART.matcher(normalized);
        int cursor = 0;
        long total = 0;
        while (matcher.find()) {
            if (matcher.start() != cursor) {
                throw invalid(input);
            }
            long amount;
            try {
                amount = Long.parseLong(matcher.group(1));
                long multiplier = switch (matcher.group(2).charAt(0)) {
                    case 's' -> 1_000L;
                    case 'm' -> 60_000L;
                    case 'h' -> 3_600_000L;
                    case 'd' -> 86_400_000L;
                    case 'w' -> 604_800_000L;
                    default -> throw invalid(input);
                };
                total = Math.addExact(total, Math.multiplyExact(amount, multiplier));
            } catch (ArithmeticException | NumberFormatException e) {
                throw invalid(input);
            }
            cursor = matcher.end();
        }
        if (cursor != normalized.length() || total <= 0) {
            throw invalid(input);
        }
        return total;
    }

    public static String formatRemaining(long remainingMillis) {
        return format(remainingMillis, 2, "expired");
    }

    /** Formats a stored moderation duration without dropping smaller non-zero units. */
    public static String formatDuration(long durationMillis) {
        return format(durationMillis, Integer.MAX_VALUE, "0s");
    }

    private static String format(long millis, int maxParts, String emptyValue) {
        if (millis <= 0) {
            return emptyValue;
        }
        long totalSeconds = Math.floorDiv(millis - 1, 1_000L) + 1;
        long[] units = {604_800L, 86_400L, 3_600L, 60L, 1L};
        String[] suffixes = {"w", "d", "h", "m", "s"};
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < units.length; i++) {
            long amount = totalSeconds / units[i];
            if (amount > 0) {
                parts.add(amount + suffixes[i]);
                totalSeconds %= units[i];
                if (parts.size() == maxParts) {
                    break;
                }
            }
        }
        return String.join(" ", parts);
    }

    private static IllegalArgumentException invalid(String input) {
        return new IllegalArgumentException(
                "Invalid duration '" + input
                        + "'. Use permanent, perm, or values such as 30m, 2h, 7d, or 1d12h.");
    }
}

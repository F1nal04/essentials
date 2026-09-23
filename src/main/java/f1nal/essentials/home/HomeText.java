package f1nal.essentials.home;

/** Placeholder substitution for configured home messages. */
public final class HomeText {
    private HomeText() {
    }

    public static String fill(String template, String name, Long seconds, Integer limit,
            Integer count) {
        String result = template == null ? "" : template;
        if (name != null) result = result.replace("{name}", name);
        if (seconds != null) result = result.replace("{seconds}", Long.toString(seconds));
        if (limit != null) result = result.replace("{limit}", Integer.toString(limit));
        if (count != null) result = result.replace("{count}", Integer.toString(count));
        return result;
    }
}

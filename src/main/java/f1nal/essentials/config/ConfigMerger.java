package f1nal.essentials.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Pure merge logic for migrating a user config to a new schema, driven by the
 * bundled default template. No Fabric or Minecraft imports so it stays unit
 * testable off-game.
 */
public final class ConfigMerger {

    public enum Status {
        NO_CHANGE,
        MERGED,
        UNREADABLE_USER,
        UNSUPPORTED_TEMPLATE
    }

    /** A dot-joined leaf key path (e.g. "backpack.mode") and its value. */
    public record Entry(String path, Object value) {
    }

    /**
     * @param status     outcome kind
     * @param reason     why, for UNREADABLE_USER / UNSUPPORTED_TEMPLATE
     * @param mergedText rewritten config text, only for MERGED
     * @param added      leaf paths new in the template, with default values
     * @param removed    leaf paths gone from the template, with old user values
     * @param reset      leaf paths whose user value could not be carried over
     *                   and reverted to the default
     */
    public record Result(Status status, String reason, String mergedText,
            List<Entry> added, List<Entry> removed, List<Entry> reset) {
    }

    private ConfigMerger() {
    }

    public static Result merge(String templateText, String userText) {
        Map<String, Object> templateLeaves = parseLeaves(templateText);
        if (templateLeaves == null) {
            return new Result(Status.UNSUPPORTED_TEMPLATE, "bundled default config is not a simple map tree",
                    null, List.of(), List.of(), List.of());
        }
        Map<String, Object> userLeaves = parseLeaves(userText);
        if (userLeaves == null) {
            return new Result(Status.UNREADABLE_USER, "not valid YAML or not a key/value tree",
                    null, List.of(), List.of(), List.of());
        }

        List<Entry> added = new ArrayList<>();
        for (Map.Entry<String, Object> e : templateLeaves.entrySet()) {
            if (!userLeaves.containsKey(e.getKey())) {
                added.add(new Entry(e.getKey(), e.getValue()));
            }
        }
        List<Entry> removed = new ArrayList<>();
        for (Map.Entry<String, Object> e : userLeaves.entrySet()) {
            if (!templateLeaves.containsKey(e.getKey())) {
                removed.add(new Entry(e.getKey(), e.getValue()));
            }
        }

        if (added.isEmpty() && removed.isEmpty()) {
            return new Result(Status.NO_CHANGE, null, null, List.of(), List.of(), List.of());
        }

        List<Entry> reset = new ArrayList<>();
        String mergedText = rewrite(templateText, templateLeaves, userLeaves, reset);
        if (mergedText == null) {
            return new Result(Status.UNSUPPORTED_TEMPLATE,
                    "bundled default config has a layout the migrator cannot rewrite",
                    null, added, removed, List.of());
        }
        return new Result(Status.MERGED, null, mergedText, added, removed, reset);
    }

    private static final Pattern KEY_LINE = Pattern.compile("^( *)([A-Za-z0-9_-]+):(.*)$");

    /**
     * Walks the template line by line so its comments and layout survive,
     * substituting the user's value on every leaf line whose key path still
     * exists. Returns null when the template has a layout the walker cannot
     * prove it handled (the caller then falls back to warn-only).
     */
    private static String rewrite(String templateText, Map<String, Object> templateLeaves,
            Map<String, Object> userLeaves, List<Entry> reset) {
        StringBuilder out = new StringBuilder();
        List<String> stack = new ArrayList<>();
        Set<String> seenLeaves = new HashSet<>();

        for (String line : templateText.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                out.append(line).append('\n');
                continue;
            }
            Matcher m = KEY_LINE.matcher(line);
            if (!m.matches()) {
                return null;
            }
            int indent = m.group(1).length();
            if (indent % 2 != 0 || indent / 2 > stack.size()) {
                return null;
            }
            while (stack.size() > indent / 2) {
                stack.remove(stack.size() - 1);
            }
            String key = m.group(2);
            if (m.group(3).isBlank()) {
                stack.add(key);
                out.append(line).append('\n');
                continue;
            }

            String path = stack.isEmpty() ? key : String.join(".", stack) + "." + key;
            seenLeaves.add(path);
            if (userLeaves.containsKey(path)) {
                String serialized = serializeScalar(userLeaves.get(path));
                if (serialized == null) {
                    reset.add(new Entry(path, userLeaves.get(path)));
                    out.append(line).append('\n');
                } else {
                    out.append(" ".repeat(indent)).append(key).append(": ").append(serialized).append('\n');
                }
            } else {
                out.append(line).append('\n');
            }
        }

        // If any template leaf never appeared as a substitutable line, the
        // walker misread the file — refuse rather than silently drop values.
        if (!seenLeaves.containsAll(templateLeaves.keySet())) {
            return null;
        }

        String result = out.toString();
        // split("\n", -1) turns a trailing newline into an extra empty segment
        return templateText.endsWith("\n") ? result.substring(0, result.length() - 1) : result;
    }

    /**
     * Renders a user value as a single-line YAML scalar that parses back to
     * the same value. Returns null when that is impossible; the caller keeps
     * the template default and reports the key as reset.
     */
    private static String serializeScalar(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }
        DumperOptions options = new DumperOptions();
        options.setWidth(Integer.MAX_VALUE);
        if (value instanceof String) {
            options.setDefaultScalarStyle(DumperOptions.ScalarStyle.DOUBLE_QUOTED);
        } else {
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
        }
        String dumped = new Yaml(options).dump(value).trim();
        return dumped.contains("\n") ? null : dumped;
    }

    /**
     * Flattens a YAML document into leaf path → value. Returns null when the
     * text is not parseable as a map tree with string keys.
     */
    private static Map<String, Object> parseLeaves(String text) {
        Object root;
        try {
            root = new Yaml(new LoaderOptions()).load(text);
        } catch (Exception e) {
            return null;
        }
        if (!(root instanceof Map<?, ?> map)) {
            return null;
        }
        Map<String, Object> leaves = new LinkedHashMap<>();
        if (!collectLeaves("", map, leaves)) {
            return null;
        }
        return leaves;
    }

    private static boolean collectLeaves(String prefix, Map<?, ?> map, Map<String, Object> out) {
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!(e.getKey() instanceof String key)) {
                return false;
            }
            String path = prefix.isEmpty() ? key : prefix + "." + key;
            if (e.getValue() instanceof Map<?, ?> child) {
                if (!collectLeaves(path, child, out)) {
                    return false;
                }
            } else {
                out.put(path, e.getValue());
            }
        }
        return true;
    }
}

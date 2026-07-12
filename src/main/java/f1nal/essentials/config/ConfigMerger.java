package f1nal.essentials.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        throw new UnsupportedOperationException("rewrite not implemented yet");
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

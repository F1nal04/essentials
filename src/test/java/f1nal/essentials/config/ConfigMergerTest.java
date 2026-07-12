package f1nal.essentials.config;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigMergerTest {

    private static final String TEMPLATE = """
            # Essentials config
            tag:
              # The chat tag
              text: "Essentials"
              bold: true

            backpack:
              # Mode: per_player | serverwide | ender_chest
              mode: "per_player"

            commands:
              repair:
                enabled: true
                access: "op"
            """;

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parse(String yaml) {
        return (Map<String, Object>) new Yaml(new LoaderOptions()).load(yaml);
    }

    @SuppressWarnings("unchecked")
    private static Object at(String yaml, String... path) {
        Object node = parse(yaml);
        for (String p : path) {
            node = ((Map<String, Object>) node).get(p);
        }
        return node;
    }

    // ---- detection ----

    @Test
    void identicalConfigIsNoChange() {
        ConfigMerger.Result r = ConfigMerger.merge(TEMPLATE, TEMPLATE);
        assertEquals(ConfigMerger.Status.NO_CHANGE, r.status());
        assertNull(r.mergedText());
        assertTrue(r.added().isEmpty());
        assertTrue(r.removed().isEmpty());
    }

    @Test
    void changedValuesOnlyIsNoChange() {
        String user = """
                tag:
                  text: "MyServer"
                  bold: false
                backpack:
                  mode: "serverwide"
                commands:
                  repair:
                    enabled: false
                    access: "all"
                """;
        ConfigMerger.Result r = ConfigMerger.merge(TEMPLATE, user);
        assertEquals(ConfigMerger.Status.NO_CHANGE, r.status());
    }

    @Test
    void garbageYamlIsUnreadable() {
        ConfigMerger.Result r = ConfigMerger.merge(TEMPLATE, "{{{ not yaml");
        assertEquals(ConfigMerger.Status.UNREADABLE_USER, r.status());
    }

    @Test
    void nonMapRootIsUnreadable() {
        assertEquals(ConfigMerger.Status.UNREADABLE_USER, ConfigMerger.merge(TEMPLATE, "just a string").status());
        assertEquals(ConfigMerger.Status.UNREADABLE_USER, ConfigMerger.merge(TEMPLATE, "").status());
    }

    // ---- rewrite ----

    @Test
    void addedKeyGetsDefaultAndUserValuesSurvive() {
        String user = """
                tag:
                  text: "MyServer"
                backpack:
                  mode: "serverwide"
                commands:
                  repair:
                    enabled: false
                    access: "all"
                """;
        ConfigMerger.Result r = ConfigMerger.merge(TEMPLATE, user);
        assertEquals(ConfigMerger.Status.MERGED, r.status());
        assertEquals(1, r.added().size());
        assertEquals("tag.bold", r.added().get(0).path());
        assertEquals(true, r.added().get(0).value());
        assertTrue(r.removed().isEmpty());

        assertEquals("MyServer", at(r.mergedText(), "tag", "text"));
        assertEquals(true, at(r.mergedText(), "tag", "bold"));
        assertEquals("serverwide", at(r.mergedText(), "backpack", "mode"));
        assertEquals(false, at(r.mergedText(), "commands", "repair", "enabled"));
        assertEquals("all", at(r.mergedText(), "commands", "repair", "access"));
    }

    @Test
    void templateCommentsSurviveTheRewrite() {
        String user = """
                tag:
                  text: "MyServer"
                """;
        ConfigMerger.Result r = ConfigMerger.merge(TEMPLATE, user);
        assertEquals(ConfigMerger.Status.MERGED, r.status());
        assertTrue(r.mergedText().contains("# The chat tag"));
        assertTrue(r.mergedText().contains("# Mode: per_player | serverwide | ender_chest"));
    }

    @Test
    void removedKeyIsDroppedAndReported() {
        String user = """
                tag:
                  text: "Essentials"
                  bold: true
                  old_thing: 12
                backpack:
                  mode: "per_player"
                commands:
                  repair:
                    enabled: true
                    access: "op"
                """;
        ConfigMerger.Result r = ConfigMerger.merge(TEMPLATE, user);
        assertEquals(ConfigMerger.Status.MERGED, r.status());
        assertEquals(1, r.removed().size());
        assertEquals("tag.old_thing", r.removed().get(0).path());
        assertEquals(12, r.removed().get(0).value());
        assertNull(((Map<?, ?>) at(r.mergedText(), "tag")).get("old_thing"));
    }

    @Test
    void mapToScalarStructureChangeCountsAsAddAndRemove() {
        String user = """
                tag:
                  text: "Essentials"
                  bold: true
                backpack: "simple"
                commands:
                  repair:
                    enabled: true
                    access: "op"
                """;
        ConfigMerger.Result r = ConfigMerger.merge(TEMPLATE, user);
        assertEquals(ConfigMerger.Status.MERGED, r.status());
        assertTrue(r.added().stream().anyMatch(e -> e.path().equals("backpack.mode")));
        assertTrue(r.removed().stream().anyMatch(e -> e.path().equals("backpack")));
        assertEquals("per_player", at(r.mergedText(), "backpack", "mode"));
    }

    @Test
    void trickyStringsRoundTrip() {
        String user = """
                tag:
                  text: "yes"
                backpack:
                  mode: "value with # hash"
                commands:
                  repair:
                    enabled: true
                    access: "op"
                """;
        ConfigMerger.Result r = ConfigMerger.merge(TEMPLATE, user);
        assertEquals(ConfigMerger.Status.MERGED, r.status());
        assertEquals("yes", at(r.mergedText(), "tag", "text"));
        assertEquals("value with # hash", at(r.mergedText(), "backpack", "mode"));
    }
}

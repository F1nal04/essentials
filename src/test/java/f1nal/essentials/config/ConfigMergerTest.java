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
}

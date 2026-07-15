package f1nal.essentials.config;

import java.util.Map;

import org.junit.jupiter.api.Test;

import f1nal.essentials.config.CommandConfig.CommandSettings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandConfigTest {

    private static final java.util.Set<String> KNOWN = java.util.Set.of(
            "repair", "heal", "feed", "flight", "disposal", "tpa", "back", "backpack", "backpacksee",
            "enderchestsee", "inventorysee", "ban", "banip", "kick", "history");

    @Test
    void missingSectionGivesAllDefaults() {
        Map<String, CommandSettings> m = CommandConfig.parse("other: {}\n");
        assertTrue(m.keySet().containsAll(KNOWN));
        assertEquals(new CommandSettings(true, "op"), m.get("repair"));
        assertEquals(new CommandSettings(true, "all"), m.get("backpack"));
        assertEquals(new CommandSettings(true, "op"), m.get("backpacksee"));
        assertEquals(new CommandSettings(true, "op"), m.get("ban"));
        assertEquals(new CommandSettings(true, "op"), m.get("banip"));
        assertEquals(new CommandSettings(true, "op"), m.get("kick"));
        assertEquals(new CommandSettings(true, "op"), m.get("history"));
    }

    @Test
    void partialEntryMergesOverDefaults() {
        Map<String, CommandSettings> m = CommandConfig.parse("commands:\n  repair:\n    enabled: false\n");
        assertFalse(m.get("repair").enabled());
        assertEquals("op", m.get("repair").access());
        assertEquals(new CommandSettings(true, "all"), m.get("tpa"));
    }

    @Test
    void unknownCommandEntryIsKept() {
        Map<String, CommandSettings> m = CommandConfig.parse("commands:\n  fly:\n    enabled: true\n    access: \"all\"\n");
        assertEquals(new CommandSettings(true, "all"), m.get("fly"));
    }

    @Test
    void nonMapEntryIsIgnored() {
        Map<String, CommandSettings> m = CommandConfig.parse("commands:\n  repair: \"broken\"\n");
        assertEquals(new CommandSettings(true, "op"), m.get("repair"));
    }

    @Test
    void wrongValueTypesFallBackPerField() {
        Map<String, CommandSettings> m = CommandConfig.parse("commands:\n  repair:\n    enabled: \"nope\"\n    access: 5\n");
        assertTrue(m.get("repair").enabled());
        assertEquals("op", m.get("repair").access());
    }

    @Test
    void garbageGivesAllDefaults() {
        Map<String, CommandSettings> m = CommandConfig.parse("{{{ not yaml");
        assertTrue(m.keySet().containsAll(KNOWN));
        assertEquals(new CommandSettings(true, "all"), m.get("disposal"));
    }

    @Test
    void legacySeeCommandKeysConfigureRenamedCommands() {
        Map<String, CommandSettings> m = CommandConfig.parse("""
                commands:
                  isee:
                    enabled: false
                    access: "all"
                  esee:
                    access: "all"
                """);
        assertEquals(new CommandSettings(false, "all"), m.get("inventorysee"));
        assertEquals(new CommandSettings(true, "all"), m.get("enderchestsee"));
    }
}

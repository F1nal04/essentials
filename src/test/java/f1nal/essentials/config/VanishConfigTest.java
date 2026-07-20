package f1nal.essentials.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VanishConfigTest {
    @Test
    void missingSectionUsesSafeDefaults() {
        VanishConfig config = VanishConfig.parse("other: true\n");
        assertTrue(config.persistState);
        assertTrue(config.hideFromTabList);
        assertTrue(config.preventMobTargeting);
        assertTrue(config.preventCollision);
        assertTrue(config.suppressAnnouncements);
        assertEquals(VanishConfig.ChatBehavior.BLOCK, config.chatBehavior);
    }

    @Test
    void configuredPoliciesAndMessagesAreRead() {
        VanishConfig config = VanishConfig.parse("""
                vanish:
                  persist_state: false
                  hide_from_tab_list: false
                  prevent_mob_targeting: false
                  prevent_collision: false
                  suppress_announcements: false
                  chat_behavior: staff_only
                  enabled_message: "hidden {player}"
                """);
        assertFalse(config.persistState);
        assertFalse(config.hideFromTabList);
        assertFalse(config.preventMobTargeting);
        assertFalse(config.preventCollision);
        assertFalse(config.suppressAnnouncements);
        assertEquals(VanishConfig.ChatBehavior.STAFF_ONLY, config.chatBehavior);
        assertEquals("hidden {player}", config.enabledMessage);
    }

    @Test
    void malformedValuesFallBackIndividually() {
        VanishConfig config = VanishConfig.parse("""
                vanish:
                  persist_state: nope
                  chat_behavior: broadcast_everywhere
                  enabled_message: ""
                """);
        assertTrue(config.persistState);
        assertEquals(VanishConfig.ChatBehavior.BLOCK, config.chatBehavior);
        assertTrue(config.enabledMessage.contains("{player}"));
    }
}

package f1nal.essentials.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackpackConfigTest {

    @Test
    void parsesAllThreeModes() {
        assertEquals(BackpackConfig.Mode.PER_PLAYER, BackpackConfig.parse("backpack:\n  mode: \"per_player\"\n").mode);
        assertEquals(BackpackConfig.Mode.SERVERWIDE, BackpackConfig.parse("backpack:\n  mode: \"serverwide\"\n").mode);
        assertEquals(BackpackConfig.Mode.ENDER_CHEST, BackpackConfig.parse("backpack:\n  mode: \"ender_chest\"\n").mode);
    }

    @Test
    void modeIsCaseInsensitive() {
        assertEquals(BackpackConfig.Mode.SERVERWIDE, BackpackConfig.parse("backpack:\n  mode: \"SERVERWIDE\"\n").mode);
    }

    @Test
    void unknownModeFallsBackToPerPlayer() {
        assertEquals(BackpackConfig.Mode.PER_PLAYER, BackpackConfig.parse("backpack:\n  mode: \"sererwide\"\n").mode);
    }

    @Test
    void missingSectionOrGarbageGivesDefaults() {
        assertEquals(BackpackConfig.Mode.PER_PLAYER, BackpackConfig.parse("other: {}\n").mode);
        assertEquals(BackpackConfig.Mode.PER_PLAYER, BackpackConfig.parse("{{{ not yaml").mode);
        assertEquals(BackpackConfig.Mode.PER_PLAYER, BackpackConfig.parse("backpack:\n  other: 1\n").mode);
    }
}

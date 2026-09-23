package f1nal.essentials.home;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HomeTeleportRulesTest {
    @Test
    void crossDimensionTravelFollowsTheSetting() {
        assertFalse(HomeTeleportRules.crossDimensionBlocked(
                "minecraft:overworld", "minecraft:the_nether", true));
        assertTrue(HomeTeleportRules.crossDimensionBlocked(
                "minecraft:overworld", "minecraft:the_nether", false));
        assertFalse(HomeTeleportRules.crossDimensionBlocked(
                "minecraft:overworld", "minecraft:overworld", false));
    }
}

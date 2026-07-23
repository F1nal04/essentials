package f1nal.essentials.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.minecraft.ChatFormatting;

class TpsConfigTest {

    @Test
    void parsesThresholdsAndColors() {
        TpsConfig config = TpsConfig.parse("""
                tps:
                  healthy:
                    minimum_tps: 19.0
                    color: "aqua"
                  degraded:
                    minimum_tps: 12.5
                    color: "GOLD"
                  critical:
                    minimum_tps: 5
                    color: "DARK_RED"
                """);

        assertEquals(19.0, config.healthy.minimumTps());
        assertEquals(ChatFormatting.AQUA, config.healthy.color());
        assertEquals(12.5, config.degraded.minimumTps());
        assertEquals(ChatFormatting.GOLD, config.degraded.color());
        assertEquals(5.0, config.critical.minimumTps());
        assertEquals(ChatFormatting.DARK_RED, config.critical.color());
    }

    @Test
    void malformedOrOutOfOrderConfigurationUsesDefaults() {
        TpsConfig malformed = TpsConfig.parse("tps:\n  healthy: nope\n");
        TpsConfig outOfOrder = TpsConfig.parse("""
                tps:
                  healthy: { minimum_tps: 14, color: GREEN }
                  degraded: { minimum_tps: 16, color: YELLOW }
                  critical: { minimum_tps: 0, color: RED }
                """);

        assertDefaults(malformed);
        assertDefaults(outOfOrder);
    }

    private static void assertDefaults(TpsConfig config) {
        assertEquals(18.0, config.healthy.minimumTps());
        assertEquals(ChatFormatting.GREEN, config.healthy.color());
        assertEquals(15.0, config.degraded.minimumTps());
        assertEquals(ChatFormatting.YELLOW, config.degraded.color());
        assertEquals(0.0, config.critical.minimumTps());
        assertEquals(ChatFormatting.RED, config.critical.color());
    }
}

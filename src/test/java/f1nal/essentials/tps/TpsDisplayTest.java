package f1nal.essentials.tps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import f1nal.essentials.tps.TpsDisplay.Health;

class TpsDisplayTest {

    @Test
    void fixesHeaderAndOneDecimalFormatting() {
        assertEquals("TPS from last 5s, 10s, 1m, 5m, 15m:", TpsDisplay.HEADER);
        assertEquals("19.5", TpsDisplay.reading(19.54, 20, 18, 15).text());
        assertEquals("19.6", TpsDisplay.reading(19.55, 20, 18, 15).text());
    }

    @Test
    void capsValuesAboveTargetAndAddsMarker() {
        TpsDisplay.Reading capped = TpsDisplay.reading(20.01, 20, 18, 15);
        TpsDisplay.Reading exact = TpsDisplay.reading(20.0, 20, 18, 15);

        assertEquals("*20.0", capped.text());
        assertTrue(capped.capped());
        assertEquals("20.0", exact.text());
        assertFalse(exact.capped());
    }

    @Test
    void thresholdBoundariesAreInclusive() {
        assertEquals(Health.HEALTHY, TpsDisplay.reading(18, 20, 18, 15).health());
        assertEquals(Health.DEGRADED, TpsDisplay.reading(15, 20, 18, 15).health());
        assertEquals(Health.CRITICAL, TpsDisplay.reading(14.999, 20, 18, 15).health());
    }

    @Test
    void loweredTargetIsUsedForCappingAndColor() {
        TpsDisplay.Reading reading = TpsDisplay.reading(20, 10, 18, 15);
        assertEquals("*10.0", reading.text());
        assertEquals(Health.CRITICAL, reading.health());
    }
}

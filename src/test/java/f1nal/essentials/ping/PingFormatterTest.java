package f1nal.essentials.ping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import f1nal.essentials.config.PingConfig.NumberFormat;
import f1nal.essentials.ping.PingFormatter.Quality;

class PingFormatterTest {
    @Test
    void thresholdBoundariesAreInclusive() {
        assertEquals(Quality.GOOD, PingFormatter.quality(100, 100, 200));
        assertEquals(Quality.MODERATE, PingFormatter.quality(101, 100, 200));
        assertEquals(Quality.MODERATE, PingFormatter.quality(200, 100, 200));
        assertEquals(Quality.POOR, PingFormatter.quality(201, 100, 200));
    }

    @Test
    void formatsIntegerLatencyAndQualityColor() {
        assertEquals("Alex: &e150 ms (moderate)", PingFormatter.format(
                "{player}: {color}{latency} ms ({quality})",
                "Alex", 150, 100, 200, NumberFormat.INTEGER));
    }

    @Test
    void formatsDecimalLatencyWithLocaleIndependentPoint() {
        assertEquals("42.0", PingFormatter.format(
                "{latency}", "Alex", 42, 100, 200, NumberFormat.DECIMAL));
    }
}

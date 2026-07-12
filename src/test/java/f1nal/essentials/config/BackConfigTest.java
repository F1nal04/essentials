package f1nal.essentials.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackConfigTest {

    @Test
    void parsesWindow() {
        assertEquals(300, BackConfig.parse("back:\n  window_seconds: 300\n").windowSeconds);
    }

    @Test
    void coercesStringNumbers() {
        assertEquals(90, BackConfig.parse("back:\n  window_seconds: \"90\"\n").windowSeconds);
    }

    @Test
    void clampsToDefaultBelowOne() {
        assertEquals(120, BackConfig.parse("back:\n  window_seconds: 0\n").windowSeconds);
        assertEquals(120, BackConfig.parse("back:\n  window_seconds: -5\n").windowSeconds);
    }

    @Test
    void missingSectionOrGarbageGivesDefaults() {
        assertEquals(120, BackConfig.parse("other: {}\n").windowSeconds);
        assertEquals(120, BackConfig.parse("{{{ not yaml").windowSeconds);
    }
}

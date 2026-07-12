package f1nal.essentials.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TpaConfigTest {

    @Test
    void parsesValues() {
        TpaConfig c = TpaConfig.parse("tpa:\n  timeout_seconds: 30\n  cooldown_seconds: 5\n");
        assertEquals(30, c.timeoutSeconds);
        assertEquals(5, c.cooldownSeconds);
    }

    @Test
    void coercesStringNumbers() {
        TpaConfig c = TpaConfig.parse("tpa:\n  timeout_seconds: \"45\"\n  cooldown_seconds: \" 7 \"\n");
        assertEquals(45, c.timeoutSeconds);
        assertEquals(7, c.cooldownSeconds);
    }

    @Test
    void clampsOutOfRangeToDefaults() {
        TpaConfig c = TpaConfig.parse("tpa:\n  timeout_seconds: 0\n  cooldown_seconds: -3\n");
        assertEquals(60, c.timeoutSeconds);
        assertEquals(10, c.cooldownSeconds);
    }

    @Test
    void zeroCooldownIsAllowed() {
        TpaConfig c = TpaConfig.parse("tpa:\n  cooldown_seconds: 0\n");
        assertEquals(0, c.cooldownSeconds);
    }

    @Test
    void missingSectionOrGarbageGivesDefaults() {
        assertEquals(60, TpaConfig.parse("other: {}\n").timeoutSeconds);
        assertEquals(60, TpaConfig.parse("{{{ not yaml").timeoutSeconds);
        assertEquals(10, TpaConfig.parse("tpa: nope\n").cooldownSeconds);
    }
}

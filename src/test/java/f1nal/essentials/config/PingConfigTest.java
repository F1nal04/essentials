package f1nal.essentials.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PingConfigTest {
    @Test
    void missingSectionUsesDefaults() {
        PingConfig config = PingConfig.parse("other: true\n");
        assertEquals(100, config.goodMaxMs);
        assertEquals(200, config.moderateMaxMs);
        assertEquals(PingConfig.NumberFormat.INTEGER, config.numberFormat);
    }

    @Test
    void readsThresholdsFormatsAndDecimalMode() {
        PingConfig config = PingConfig.parse("""
                ping:
                  good_max_ms: 80
                  moderate_max_ms: 160
                  number_format: "decimal"
                  self_format: "Ping: {latency}"
                  console_requires_player_format: "Choose a player"
                """);
        assertEquals(80, config.goodMaxMs);
        assertEquals(160, config.moderateMaxMs);
        assertEquals(PingConfig.NumberFormat.DECIMAL, config.numberFormat);
        assertEquals("Ping: {latency}", config.selfFormat);
        assertEquals("Choose a player", config.consoleRequiresPlayerFormat);
    }

    @Test
    void invalidValuesFallBackAndThresholdsStayOrdered() {
        PingConfig config = PingConfig.parse("""
                ping:
                  good_max_ms: 150
                  moderate_max_ms: 100
                  number_format: "fractional"
                """);
        assertEquals(150, config.goodMaxMs);
        assertEquals(150, config.moderateMaxMs);
        assertEquals(PingConfig.NumberFormat.INTEGER, config.numberFormat);
    }
}

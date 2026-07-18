package f1nal.essentials.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import f1nal.essentials.update.ReleaseChannel;

class UpdateConfigTest {

    @Test
    void defaultsAreSafeAndStableOnly() {
        UpdateConfig config = UpdateConfig.parse("other: true\n");
        assertTrue(config.enabled());
        assertEquals(ReleaseChannel.STABLE_ONLY, config.channel());
        assertTrue(config.consoleNotifications());
        assertTrue(config.playerNotifications());
        assertTrue(config.opFallback());
        assertEquals(5, config.requestTimeoutSeconds());
        assertEquals(10, config.startupDelaySeconds());
    }

    @Test
    void parsesSwitchesAndClampsNetworkTiming() {
        UpdateConfig config = UpdateConfig.parse("""
                updates:
                  enabled: false
                  channel: include_prereleases
                  console_notifications: false
                  player_notifications: false
                  op_fallback: false
                  request_timeout_seconds: 999
                  startup_delay_seconds: -5
                  notification_text: "Version {latest_version}"
                  clickable_link: false
                """);
        assertFalse(config.enabled());
        assertEquals(ReleaseChannel.INCLUDE_PRERELEASES, config.channel());
        assertFalse(config.consoleNotifications());
        assertFalse(config.playerNotifications());
        assertFalse(config.opFallback());
        assertEquals(UpdateConfig.MAX_TIMEOUT_SECONDS, config.requestTimeoutSeconds());
        assertEquals(UpdateConfig.MIN_STARTUP_DELAY_SECONDS, config.startupDelaySeconds());
        assertEquals("Version {latest_version}", config.notificationText());
        assertFalse(config.clickableLink());
    }
}

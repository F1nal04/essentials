package f1nal.essentials.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagingConfigTest {
    @Test
    void missingSectionUsesDefaults() {
        MessagingConfig config = MessagingConfig.parse("other: true\n");
        assertTrue(config.incomingFormat.contains("{sender}"));
        assertTrue(config.staffBypassesIgnore);
        assertTrue(config.consoleBypassesIgnore);
    }

    @Test
    void configuredFormatsAndPoliciesAreRead() {
        MessagingConfig config = MessagingConfig.parse("""
                messaging:
                  incoming_format: "from {sender}: {message}"
                  reply_format: "reply to {recipient}: {message}"
                  staff_bypasses_ignore: false
                  console_bypasses_ignore: false
                """);
        assertEquals("from {sender}: {message}", config.incomingFormat);
        assertEquals("reply to {recipient}: {message}", config.replyFormat);
        assertFalse(config.staffBypassesIgnore);
        assertFalse(config.consoleBypassesIgnore);
    }
}

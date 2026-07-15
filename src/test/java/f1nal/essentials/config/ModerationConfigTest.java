package f1nal.essentials.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ModerationConfigTest {

    @Test
    void parsesConfiguredMessagesAndNewlines() {
        ModerationConfig config = ModerationConfig.parse("""
                moderation:
                  ban_message: "Ban: {reason}\\nLeft: {time}"
                  kick_message: "Kick: {reason}"
                """);

        assertEquals("Ban: {reason}\nLeft: {time}", config.banMessage);
        assertEquals("Kick: {reason}", config.kickMessage);
    }

    @Test
    void missingAndBlankFieldsUseDefaults() {
        ModerationConfig config = ModerationConfig.parse("""
                moderation:
                  ban_message: ""
                """);

        assertTrue(config.banMessage.contains("{reason}"));
        assertTrue(config.kickMessage.contains("{reason}"));
    }

    @Test
    void messagesMissingRequiredPlaceholdersUseDefaults() {
        ModerationConfig config = ModerationConfig.parse("""
                moderation:
                  ban_message: "No details"
                  kick_message: "No details"
                """);

        assertTrue(config.banMessage.contains("{reason}"));
        assertTrue(config.banMessage.contains("{time}"));
        assertTrue(config.kickMessage.contains("{reason}"));
    }
}

package f1nal.essentials.config;

import net.minecraft.ChatFormatting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagConfigTest {

    private static final String VALID = """
            tag:
              text: "MyTag"
              color: "aqua"
              bracketColor: "GRAY"
              bold: false
            """;

    @Test
    void parsesAllFields() {
        TagConfig c = TagConfig.parse(VALID);
        assertEquals("MyTag", c.text);
        assertEquals(ChatFormatting.AQUA, c.color);
        assertEquals(ChatFormatting.GRAY, c.bracketColor);
        assertFalse(c.bold);
    }

    @Test
    void anyMissingFieldGivesFullDefaults() {
        TagConfig c = TagConfig.parse("tag:\n  text: \"MyTag\"\n  color: \"AQUA\"\n  bold: true\n");
        assertEquals("Essentials", c.text);
        assertEquals(ChatFormatting.DARK_PURPLE, c.color);
        assertEquals(ChatFormatting.DARK_GRAY, c.bracketColor);
        assertTrue(c.bold);
    }

    @Test
    void unknownColorGivesFullDefaults() {
        TagConfig c = TagConfig.parse(VALID.replace("aqua", "not_a_color"));
        assertEquals("Essentials", c.text);
        assertEquals(ChatFormatting.DARK_PURPLE, c.color);
    }

    @Test
    void nonBooleanBoldGivesFullDefaults() {
        TagConfig c = TagConfig.parse(VALID.replace("bold: false", "bold: \"false\""));
        assertEquals("Essentials", c.text);
        assertTrue(c.bold);
    }

    @Test
    void missingSectionOrGarbageGivesDefaults() {
        assertEquals("Essentials", TagConfig.parse("other: {}\n").text);
        assertEquals("Essentials", TagConfig.parse("{{{ not yaml").text);
    }
}

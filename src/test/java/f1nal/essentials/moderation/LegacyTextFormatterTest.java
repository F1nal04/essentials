package f1nal.essentials.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

class LegacyTextFormatterTest {

    @Test
    void appliesColorAndStyleCodesWithoutChangingText() {
        Component component = LegacyTextFormatter.parse("&cRed &lBold && literal");

        assertEquals("Red Bold & literal", component.getString());
        assertEquals(TextColor.RED, component.getSiblings().get(0).getStyle().getColor());
        assertTrue(component.getSiblings().get(1).getStyle().isBold());
    }
}

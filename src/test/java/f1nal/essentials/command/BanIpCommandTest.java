package f1nal.essentials.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;

import org.junit.jupiter.api.Test;

class BanIpCommandTest {

    @Test
    void quotedStringArgumentAcceptsIpv6() throws Exception {
        StringReader reader = new StringReader("\"2001:db8::10\" 1h Proxy");

        assertEquals("2001:db8::10", StringArgumentType.string().parse(reader));
        assertEquals(' ', reader.peek());
    }
}

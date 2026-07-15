package f1nal.essentials.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.brigadier.StringReader;

import org.junit.jupiter.api.Test;

class IpBanTargetArgumentTest {

    @Test
    void acceptsIpv4Ipv6AndPlayerNamesUntilWhitespace() throws Exception {
        assertEquals("192.0.2.10",
                IpBanTargetArgument.target().parse(new StringReader("192.0.2.10 1h reason")));
        assertEquals("2001:db8::10",
                IpBanTargetArgument.target().parse(new StringReader("2001:db8::10 1h reason")));
        assertEquals("PlayerName",
                IpBanTargetArgument.target().parse(new StringReader("PlayerName 1h reason")));
    }
}

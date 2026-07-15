package f1nal.essentials.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.junit.jupiter.api.Test;

class IpAddressUtilTest {

    @Test
    void normalizesIpv4AndIpv6WithoutDns() {
        assertEquals("192.0.2.10", IpAddressUtil.normalizeLiteral("192.0.2.10"));
        assertEquals("2001:db8::1", IpAddressUtil.normalizeLiteral("2001:0db8:0:0:0:0:0:1"));
    }

    @Test
    void rejectsHostnamesAndMalformedAddresses() {
        assertThrows(IllegalArgumentException.class,
                () -> IpAddressUtil.normalizeLiteral("example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> IpAddressUtil.normalizeLiteral("999.1.1.1"));
    }

    @Test
    void extractsNormalizedAddressFromConnection() throws Exception {
        InetAddress address = InetAddress.getByAddress(new byte[] {(byte) 192, 0, 2, 10});
        assertEquals("192.0.2.10", IpAddressUtil.fromSocketAddress(
                new InetSocketAddress(address, 25565)).orElseThrow());
    }
}

package f1nal.essentials.moderation;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;

import com.google.common.net.InetAddresses;

public final class IpAddressUtil {

    private IpAddressUtil() {
    }

    /** Parses an IPv4 or IPv6 literal without performing a DNS lookup. */
    public static String normalizeLiteral(String input) {
        try {
            return InetAddresses.toAddrString(InetAddresses.forString(input.trim()));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Invalid IP address '" + input + "'.", e);
        }
    }

    public static Optional<String> fromSocketAddress(SocketAddress socketAddress) {
        if (!(socketAddress instanceof InetSocketAddress inetAddress)
                || inetAddress.getAddress() == null) {
            return Optional.empty();
        }
        return Optional.of(InetAddresses.toAddrString(inetAddress.getAddress()));
    }
}

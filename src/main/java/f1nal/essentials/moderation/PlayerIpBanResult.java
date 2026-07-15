package f1nal.essentials.moderation;

import java.util.Objects;

public record PlayerIpBanResult(BanRecord playerBan, IpBanRecord ipBan) {

    public PlayerIpBanResult {
        Objects.requireNonNull(playerBan, "playerBan");
        Objects.requireNonNull(ipBan, "ipBan");
    }
}

package f1nal.essentials.moderation;

import java.util.Objects;
import java.util.UUID;

public record IpBanRecord(
        long id,
        String address,
        UUID targetUuid,
        String targetName,
        String reason,
        long issuedAtMs,
        Long expiresAtMs,
        UUID moderatorUuid,
        String moderatorName) {

    public IpBanRecord {
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(moderatorName, "moderatorName");
        if ((targetUuid == null) != (targetName == null)) {
            throw new IllegalArgumentException("IP-ban target UUID and name must both be present or absent");
        }
        if (expiresAtMs != null && expiresAtMs <= issuedAtMs) {
            throw new IllegalArgumentException("IP-ban expiration must be after its issue time");
        }
    }

    public String targetDisplay() {
        return targetName == null ? address : targetName;
    }

    public boolean permanent() {
        return expiresAtMs == null;
    }
}

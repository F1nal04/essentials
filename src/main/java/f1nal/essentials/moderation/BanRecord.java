package f1nal.essentials.moderation;

import java.util.Objects;
import java.util.UUID;

public record BanRecord(
        long id,
        UUID targetUuid,
        String targetName,
        String reason,
        long issuedAtMs,
        long expiresAtMs,
        UUID moderatorUuid,
        String moderatorName) {

    public BanRecord {
        Objects.requireNonNull(targetUuid, "targetUuid");
        Objects.requireNonNull(targetName, "targetName");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(moderatorName, "moderatorName");
        if (expiresAtMs <= issuedAtMs) {
            throw new IllegalArgumentException("Ban expiration must be after its issue time");
        }
    }
}

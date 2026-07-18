package f1nal.essentials.moderation;

import java.util.UUID;

public record MuteRecord(
        long id,
        UUID targetUuid,
        String targetName,
        String reason,
        long issuedAtMs,
        Long expiresAtMs,
        UUID moderatorUuid,
        String moderatorName) {

    public boolean permanent() {
        return expiresAtMs == null;
    }
}

package f1nal.essentials.moderation;

import java.util.UUID;

public record AuditRecord(
        long id,
        Action action,
        UUID targetUuid,
        String targetName,
        String address,
        String reason,
        long occurredAtMs,
        Long expiresAtMs,
        UUID moderatorUuid,
        String moderatorName,
        String state,
        Long revokedAtMs,
        UUID revokedByUuid,
        String revokedByName) {

    public enum Action {
        BAN,
        IP_BAN,
        KICK
    }

    public long durationMs() {
        if (action == Action.KICK || expiresAtMs == null) {
            return 0;
        }
        return expiresAtMs - occurredAtMs;
    }
}

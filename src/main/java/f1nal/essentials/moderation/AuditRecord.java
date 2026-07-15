package f1nal.essentials.moderation;

import java.util.UUID;

public record AuditRecord(
        long id,
        Action action,
        UUID targetUuid,
        String targetName,
        String reason,
        long occurredAtMs,
        Long expiresAtMs,
        UUID moderatorUuid,
        String moderatorName,
        String state) {

    public enum Action {
        BAN,
        KICK
    }

    public long durationMs() {
        if (action != Action.BAN || expiresAtMs == null) {
            return 0;
        }
        return expiresAtMs - occurredAtMs;
    }
}

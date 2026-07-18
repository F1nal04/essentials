package f1nal.essentials.moderation;

import java.util.UUID;

public record WarningRecord(
        long id,
        UUID targetUuid,
        String targetName,
        String reason,
        long issuedAtMs,
        UUID moderatorUuid,
        String moderatorName) {
}

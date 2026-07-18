package f1nal.essentials.moderation;

import java.util.UUID;

public record StaffNoteRecord(
        long id,
        UUID targetUuid,
        String targetName,
        String text,
        long createdAtMs,
        UUID moderatorUuid,
        String moderatorName) {
}

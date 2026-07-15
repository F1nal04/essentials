package f1nal.essentials.moderation;

import java.util.List;

public record AuditPage(List<AuditRecord> records, long totalRecords) {

    public AuditPage {
        records = List.copyOf(records);
        if (totalRecords < 0) {
            throw new IllegalArgumentException("Total record count cannot be negative");
        }
    }
}

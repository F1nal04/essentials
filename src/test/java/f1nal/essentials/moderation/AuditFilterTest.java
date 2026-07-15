package f1nal.essentials.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class AuditFilterTest {

    @Test
    void parsesSupportedFiltersCaseInsensitively() {
        assertEquals(AuditFilter.ALL, AuditFilter.parse("all"));
        assertEquals(AuditFilter.BANS, AuditFilter.parse("BANS"));
        assertEquals(AuditFilter.KICKS, AuditFilter.parse("kicks"));
    }

    @Test
    void rejectsUnknownFilters() {
        assertThrows(IllegalArgumentException.class, () -> AuditFilter.parse("mutes"));
    }
}

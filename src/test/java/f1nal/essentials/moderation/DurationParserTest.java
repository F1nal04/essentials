package f1nal.essentials.moderation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DurationParserTest {

    @Test
    void parsesSingleAndCompoundDurations() {
        assertEquals(30L * 60_000L, DurationParser.parseMillis("30m"));
        assertEquals(2L * 3_600_000L, DurationParser.parseMillis("2H"));
        assertEquals(129_600_000L, DurationParser.parseMillis("1d12h"));
    }

    @Test
    void rejectsMissingMalformedZeroAndOverflowingDurations() {
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseMillis(""));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseMillis("12"));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseMillis("1hour"));
        assertThrows(IllegalArgumentException.class, () -> DurationParser.parseMillis("0s"));
        assertThrows(IllegalArgumentException.class,
                () -> DurationParser.parseMillis("999999999999999999999w"));
    }

    @Test
    void formatsRemainingTimeWithTwoLargestUnits() {
        assertEquals("1d 2h", DurationParser.formatRemaining(93_600_000L));
        assertEquals("1s", DurationParser.formatRemaining(1L));
        assertEquals("expired", DurationParser.formatRemaining(0L));
    }
}

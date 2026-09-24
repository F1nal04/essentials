package f1nal.essentials.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

class HomeLimitsTest {
    @Test
    void missingPermissionsUseTheConfiguredDefault() {
        int limit = HomeLimits.resolve(1, 5, false, number -> false);
        assertEquals(1, limit);
        assertTrue(HomeLimits.canCreate(0, limit));
        assertFalse(HomeLimits.canCreate(1, limit));
    }

    @Test
    void highestValidNumericPermissionWins() {
        Set<Integer> granted = Set.of(2, 4, 9);
        int limit = HomeLimits.resolve(1, 5, false, granted::contains);
        assertEquals(4, limit);
        assertTrue(HomeLimits.canCreate(3, limit));
        assertFalse(HomeLimits.canCreate(4, limit));
    }

    @Test
    void numericPermissionReplacesAHigherDefault() {
        int limit = HomeLimits.resolve(3, 5, false, number -> number == 2);
        assertEquals(2, limit);
    }

    @Test
    void unlimitedBypassesTheConfiguredMaximum() {
        int limit = HomeLimits.resolve(1, 5, true, number -> false);
        assertEquals(HomeLimits.UNLIMITED, limit);
        assertTrue(HomeLimits.canCreate(100, limit));
    }
}

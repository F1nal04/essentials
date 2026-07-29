package f1nal.essentials.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SafePositionFinderTest {
    @Test
    void keepsConfiguredPositionWhenItIsSafe() {
        var result = SafePositionFinder.find(10.25, 64, -4.75,
                position -> position.x() == 10.25 && position.y() == 64);
        assertEquals(new SafePositionFinder.Position(10.25, 64, -4.75),
                result.orElseThrow());
    }

    @Test
    void choosesNearestVerticalSafePositionFirst() {
        var result = SafePositionFinder.find(0, 64, 0,
                position -> position.y() == 63);
        assertEquals(new SafePositionFinder.Position(0, 63, 0), result.orElseThrow());
    }

    @Test
    void fallsBackToBlockAlignedHeightFromFractionalConfiguredHeight() {
        var result = SafePositionFinder.find(0.25, 64.5, 0.75,
                position -> position.y() == 65);
        assertEquals(new SafePositionFinder.Position(0.25, 65, 0.75),
                result.orElseThrow());
    }

    @Test
    void searchesNearbyColumnsWhenConfiguredColumnIsUnsafe() {
        var result = SafePositionFinder.find(10.2, 64, 20.8,
                position -> position.x() == 9.5 && position.z() == 19.5
                        && position.y() == 64);
        assertEquals(new SafePositionFinder.Position(9.5, 64, 19.5),
                result.orElseThrow());
    }

    @Test
    void returnsEmptyWhenNoCandidateIsSafe() {
        assertTrue(SafePositionFinder.find(0, 64, 0, position -> false).isEmpty());
    }
}

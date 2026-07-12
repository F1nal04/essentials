package f1nal.essentials.back;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackPositionsTest {

    private long now = 1_000_000L;
    private final BackPositions<String> positions = new BackPositions<>(() -> now, () -> 10_000L);
    private final UUID player = UUID.randomUUID();

    @Test
    void peekIsRepeatable() {
        positions.mark(player, "spawn");
        assertEquals(Optional.of("spawn"), positions.peek(player));
        assertEquals(Optional.of("spawn"), positions.peek(player));
    }

    @Test
    void consumeIsOneShot() {
        positions.mark(player, "spawn");
        assertEquals(Optional.of("spawn"), positions.consume(player));
        assertTrue(positions.consume(player).isEmpty());
        assertTrue(positions.peek(player).isEmpty());
    }

    @Test
    void entryExpiresExactlyAtWindowBoundary() {
        positions.mark(player, "spawn");
        now += 9_999;
        assertTrue(positions.peek(player).isPresent());
        now += 1;
        assertTrue(positions.peek(player).isEmpty());
        assertTrue(positions.consume(player).isEmpty());
    }

    @Test
    void remarkReplacesPositionAndRestartsWindow() {
        positions.mark(player, "first");
        now += 8_000;
        positions.mark(player, "second");
        now += 8_000;
        assertEquals(Optional.of("second"), positions.peek(player));
    }

    @Test
    void cleanupDropsOnlyExpiredEntries() {
        UUID other = UUID.randomUUID();
        positions.mark(player, "old");
        now += 5_000;
        positions.mark(other, "young");
        now += 5_000;
        positions.cleanup();
        assertTrue(positions.peek(player).isEmpty());
        assertEquals(Optional.of("young"), positions.peek(other));
    }
}

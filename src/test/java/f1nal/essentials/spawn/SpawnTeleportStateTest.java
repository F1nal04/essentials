package f1nal.essentials.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import f1nal.essentials.spawn.SpawnTeleportState.Origin;
import f1nal.essentials.spawn.SpawnTeleportState.RequestResult;
import f1nal.essentials.spawn.SpawnTeleportState.TickResult;

class SpawnTeleportStateTest {
    private final AtomicLong now = new AtomicLong(1_000);
    private final SpawnTeleportState state = new SpawnTeleportState(now::get);
    private UUID player;
    private Origin origin;

    @BeforeEach
    void setUp() {
        player = UUID.randomUUID();
        origin = new Origin("minecraft:overworld", 1, 64, 2);
    }

    @Test
    void warmupBecomesReadyAfterConfiguredDelay() {
        var request = state.request(player, origin, 3_000, false);
        assertEquals(RequestResult.WARMING_UP, request.result());
        assertEquals(3, request.seconds());
        assertEquals(TickResult.NONE, state.tick(player, origin, true));

        now.addAndGet(3_000);

        assertEquals(TickResult.READY, state.tick(player, origin, true));
    }

    @Test
    void movementAndDimensionChangesCancelWarmup() {
        state.request(player, origin, 3_000, false);
        assertEquals(TickResult.MOVED, state.tick(player,
                new Origin("minecraft:overworld", 1.2, 64, 2), true));

        state.request(player, origin, 3_000, false);
        assertEquals(TickResult.MOVED, state.tick(player,
                new Origin("minecraft:the_nether", 1, 64, 2), true));
    }

    @Test
    void movementCanBeAllowedAndDamageCanCancel() {
        state.request(player, origin, 3_000, false);
        assertEquals(TickResult.NONE, state.tick(player,
                new Origin("minecraft:overworld", 50, 80, 50), false));
        assertTrue(state.damage(player, true));
        assertFalse(state.damage(player, true));
    }

    @Test
    void completedTeleportStartsCooldownWithBypass() {
        assertEquals(RequestResult.READY,
                state.request(player, origin, 0, false).result());
        state.complete(player, 5_000);

        var blocked = state.request(player, origin, 0, false);
        assertEquals(RequestResult.COOLDOWN, blocked.result());
        assertEquals(5, blocked.seconds());
        assertEquals(RequestResult.READY,
                state.request(player, origin, 0, true).result());
    }

    @Test
    void duplicatePendingRequestIsRejected() {
        state.request(player, origin, 3_000, false);
        assertEquals(RequestResult.ALREADY_PENDING,
                state.request(player, origin, 3_000, false).result());
    }
}

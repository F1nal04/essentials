package f1nal.essentials.spawn;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpawnRoutingTest {
    @Test
    void firstJoinRequiresFeatureFirstJoinAndConfiguredSpawn() {
        assertTrue(SpawnRouting.shouldRouteFirstJoin(true, true, true));
        assertFalse(SpawnRouting.shouldRouteFirstJoin(false, true, true));
        assertFalse(SpawnRouting.shouldRouteFirstJoin(true, false, true));
        assertFalse(SpawnRouting.shouldRouteFirstJoin(true, true, false));
    }

    @Test
    void deathRespawnRequiresSpawnAndNoPersonalRespawnPoint() {
        assertTrue(SpawnRouting.shouldRouteRespawn(true, false, true, false));
        assertFalse(SpawnRouting.shouldRouteRespawn(false, false, true, false));
        assertFalse(SpawnRouting.shouldRouteRespawn(true, true, true, false));
        assertFalse(SpawnRouting.shouldRouteRespawn(true, false, false, false));
        assertFalse(SpawnRouting.shouldRouteRespawn(true, false, true, true));
    }
}

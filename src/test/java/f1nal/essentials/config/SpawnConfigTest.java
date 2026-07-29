package f1nal.essentials.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpawnConfigTest {
    @Test
    void parsesRoutingTimingCancellationAndMessages() {
        SpawnConfig config = SpawnConfig.parse("""
                spawn:
                  first_join: true
                  respawn: true
                  warmup_seconds: "7"
                  cooldown_seconds: 45
                  cancel_on_movement: false
                  cancel_on_damage: false
                  teleported_message: "&bArrived."
                """);

        assertTrue(config.firstJoin);
        assertTrue(config.respawn);
        assertEquals(7, config.warmupSeconds);
        assertEquals(45, config.cooldownSeconds);
        assertFalse(config.cancelOnMovement);
        assertFalse(config.cancelOnDamage);
        assertEquals("&bArrived.", config.teleportedMessage);
    }

    @Test
    void invalidValuesFallBackPerField() {
        SpawnConfig config = SpawnConfig.parse("""
                spawn:
                  first_join: "yes"
                  warmup_seconds: -1
                  cooldown_seconds: 999999
                  no_spawn_message: ""
                """);

        assertTrue(config.firstJoin);
        assertEquals(3, config.warmupSeconds);
        assertEquals(30, config.cooldownSeconds);
        assertEquals("&cNo Essentials spawn has been configured.", config.noSpawnMessage);
    }

    @Test
    void missingSectionAndMalformedYamlUseDefaults() {
        SpawnConfig missing = SpawnConfig.parse("other: {}\n");
        assertTrue(missing.firstJoin);
        assertTrue(missing.respawn);
        assertEquals(3, missing.warmupSeconds);
        assertEquals(30, SpawnConfig.parse("{{{ not yaml").cooldownSeconds);
    }
}

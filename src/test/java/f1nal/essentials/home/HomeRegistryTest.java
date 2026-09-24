package f1nal.essentials.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class HomeRegistryTest {
    private final HomePoint overworld = new HomePoint(
            "minecraft:overworld", 10.5, 64, -3.25, 90f, -12.5f);
    private final HomePoint nether = new HomePoint(
            "minecraft:the_nether", 1, 70, 2, 180f, 0f);

    @Test
    void defaultHomeCanBeSetListedVisitedAndDeleted() {
        HomeRegistry registry = new HomeRegistry();
        UUID player = UUID.randomUUID();

        assertEquals(HomeRegistry.SetStatus.CREATED,
                registry.set(player, "home", overworld, 1));
        assertEquals(List.of("home"), registry.names(player));
        assertEquals(overworld, registry.find(player, "HOME").orElseThrow());
        assertEquals(HomeRegistry.DeleteStatus.DELETED, registry.delete(player, "home"));
        assertTrue(registry.names(player).isEmpty());
        assertEquals(HomeRegistry.DeleteStatus.MISSING, registry.delete(player, "home"));
    }

    @Test
    void namedHomesAreUniquePerOwnerAndRespectTheLimit() {
        HomeRegistry registry = new HomeRegistry();
        UUID player = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        assertEquals(HomeRegistry.SetStatus.CREATED, registry.set(player, "base", overworld, 2));
        assertEquals(HomeRegistry.SetStatus.CREATED, registry.set(player, "Mine", nether, 2));
        assertEquals(HomeRegistry.SetStatus.DUPLICATE, registry.set(player, "mine", overworld, 2));
        assertEquals(HomeRegistry.SetStatus.LIMIT_REACHED,
                registry.set(player, "farm", overworld, 2));
        assertEquals(List.of("base", "mine"), registry.names(player));
        assertEquals(nether, registry.find(player, "mine").orElseThrow());

        assertEquals(HomeRegistry.SetStatus.CREATED, registry.set(other, "mine", overworld, 1));
        assertEquals(overworld, registry.find(other, "mine").orElseThrow());
        assertEquals(1, registry.count(other));
    }

    @Test
    void reloadKeepsTheSameOwnerAfterADisplayNameWouldHaveChanged() {
        UUID owner = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        HomeRegistry original = new HomeRegistry();
        original.set(owner, "cottage", overworld, 1);

        HomeRegistry reloaded = new HomeRegistry();
        reloaded.replace(original.entries());

        StoredHome stored = reloaded.entries().get(0);
        assertEquals(owner, stored.owner());
        assertEquals("cottage", stored.name());
        assertEquals("minecraft:overworld", stored.point().dimension());
        assertEquals(10.5, stored.point().x());
        assertEquals(90f, stored.point().yaw());
        assertEquals(-12.5f, stored.point().pitch());
        assertTrue(reloaded.find(UUID.randomUUID(), "cottage").isEmpty());
    }
}

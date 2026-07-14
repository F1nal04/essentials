package f1nal.essentials.command;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryViewLayoutTest {

    @Test
    void coversEveryInventorySlotExactlyOnce() {
        Set<Integer> seen = new HashSet<>();
        int locked = 0;
        for (int view = 0; view < InventoryViewLayout.VIEW_SIZE; view++) {
            int inv = InventoryViewLayout.mapSlot(view);
            if (inv == InventoryViewLayout.LOCKED) {
                locked++;
                continue;
            }
            assertTrue(inv >= 0 && inv <= 40, "inventory index out of range: " + inv);
            assertTrue(seen.add(inv), "duplicate mapping for inventory slot " + inv);
        }
        assertEquals(41, seen.size());
        assertEquals(13, locked);
    }

    @Test
    void spotChecksMatchTheIntendedLayout() {
        assertEquals(9, InventoryViewLayout.mapSlot(0));    // first storage slot
        assertEquals(35, InventoryViewLayout.mapSlot(26));  // last storage slot
        assertEquals(InventoryViewLayout.LOCKED, InventoryViewLayout.mapSlot(27));
        assertEquals(InventoryViewLayout.LOCKED, InventoryViewLayout.mapSlot(35));
        assertEquals(0, InventoryViewLayout.mapSlot(36));   // hotbar row starts
        assertEquals(8, InventoryViewLayout.mapSlot(44));   // hotbar row ends
        assertEquals(39, InventoryViewLayout.mapSlot(45));  // helmet
        assertEquals(38, InventoryViewLayout.mapSlot(46));  // chestplate
        assertEquals(37, InventoryViewLayout.mapSlot(47));  // leggings
        assertEquals(36, InventoryViewLayout.mapSlot(48));  // boots
        assertEquals(InventoryViewLayout.LOCKED, InventoryViewLayout.mapSlot(49));
        assertEquals(InventoryViewLayout.LOCKED, InventoryViewLayout.mapSlot(52));
        assertEquals(40, InventoryViewLayout.mapSlot(53));  // offhand
    }

    @Test
    void outOfRangeThrows() {
        assertThrows(IllegalArgumentException.class, () -> InventoryViewLayout.mapSlot(-1));
        assertThrows(IllegalArgumentException.class, () -> InventoryViewLayout.mapSlot(54));
    }
}

package f1nal.essentials.command;

/**
 * Maps the 45 view slots of the /inventorysee chest UI onto player inventory slot
 * indices. Pure JVM (no Minecraft imports) so it stays unit testable.
 *
 * View layout: rows 1-3 = main storage (inventory 9-35), row 4 = hotbar
 * (0-8), row 5 = helmet, chestplate, leggings, boots (39..36), offhand (40),
 * then four locked fillers.
 */
public final class InventoryViewLayout {

    public static final int VIEW_SIZE = 45;
    public static final int LOCKED = -1;

    private InventoryViewLayout() {
    }

    public static int mapSlot(int viewIndex) {
        if (viewIndex < 0 || viewIndex >= VIEW_SIZE) {
            throw new IllegalArgumentException("view index out of range: " + viewIndex);
        }
        if (viewIndex < 27) {
            return viewIndex + 9;         // rows 1-3: main storage 9-35
        }
        if (viewIndex < 36) {
            return viewIndex - 27;        // row 4: hotbar 0-8
        }
        if (viewIndex < 40) {
            return 39 - (viewIndex - 36); // row 5: helmet 39 down to boots 36
        }
        if (viewIndex == 40) {
            return 40;                    // offhand
        }
        return LOCKED;                    // 41-44: locked fillers
    }
}

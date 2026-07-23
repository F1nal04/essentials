package f1nal.essentials.ping;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PingAccessTest {
    @Test
    void consoleCanAlwaysUseTargetedForm() {
        assertTrue(PingAccess.canTargetOther(false, false));
    }

    @Test
    void consoleCannotUseSelfForm() {
        assertFalse(PingAccess.canUseSelf(false));
        assertTrue(PingAccess.canUseSelf(true));
    }

    @Test
    void playerRequiresOthersPermission() {
        assertTrue(PingAccess.canTargetOther(true, true));
        assertFalse(PingAccess.canTargetOther(true, false));
    }

    @Test
    void hiddenOrOfflineTargetIsUnavailable() {
        assertTrue(PingAccess.targetAvailable(true, true));
        assertFalse(PingAccess.targetAvailable(true, false));
        assertFalse(PingAccess.targetAvailable(false, true));
    }
}

package f1nal.essentials.permission;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PermissionDecisionTest {

    @Test
    void providerGrantOverridesLegacyDenial() {
        assertTrue(PermissionDecision.resolve(Boolean.TRUE, false));
    }

    @Test
    void providerDenialOverridesLegacyGrant() {
        assertFalse(PermissionDecision.resolve(Boolean.FALSE, true));
    }

    @Test
    void absentProviderUsesLegacyOpOrAllResult() {
        assertTrue(PermissionDecision.resolve(null, true));
        assertFalse(PermissionDecision.resolve(null, false));
    }

    @Test
    void subPermissionUsesTheSameProviderOverrideAndLegacyFallback() {
        assertTrue(PermissionDecision.resolve(Boolean.TRUE, false));
        assertFalse(PermissionDecision.resolve(Boolean.FALSE, true));
        assertTrue(PermissionDecision.resolve(null, true));
    }

    @Test
    void consoleAlwaysRetainsLegacyBehavior() {
        assertTrue(PermissionDecision.resolve(Boolean.FALSE, true, false));
        assertFalse(PermissionDecision.resolve(Boolean.TRUE, false, false));
    }

    @Test
    void updateNotificationUsesProviderDecisionThenOpFallback() {
        assertTrue(PermissionDecision.resolve(Boolean.TRUE, false, true));
        assertFalse(PermissionDecision.resolve(Boolean.FALSE, true, true));
        assertTrue(PermissionDecision.resolve(null, true, true));
        assertFalse(PermissionDecision.resolve(null, false, true));
    }
}

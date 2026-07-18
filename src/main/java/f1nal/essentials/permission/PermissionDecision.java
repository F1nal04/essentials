package f1nal.essentials.permission;

/** Resolves a provider response without changing the legacy fallback. */
public final class PermissionDecision {

    private PermissionDecision() {
    }

    public static boolean resolve(Boolean providerResult, boolean legacyFallback) {
        return providerResult != null ? providerResult : legacyFallback;
    }

    public static boolean resolve(
            Boolean providerResult,
            boolean legacyFallback,
            boolean playerSource) {
        return playerSource ? resolve(providerResult, legacyFallback) : legacyFallback;
    }
}

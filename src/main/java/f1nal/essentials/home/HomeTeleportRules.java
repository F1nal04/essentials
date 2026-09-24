package f1nal.essentials.home;

/** Pure checks for whether a home teleport may cross dimensions. */
public final class HomeTeleportRules {
    private HomeTeleportRules() {
    }

    public static boolean crossDimensionBlocked(String currentDimension, String homeDimension,
            boolean allowCrossDimension) {
        if (allowCrossDimension) return false;
        if (currentDimension == null || homeDimension == null) return true;
        return !currentDimension.equals(homeDimension);
    }
}

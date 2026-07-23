package f1nal.essentials.ping;

/** Minecraft-free policy decisions used by /ping. */
public final class PingAccess {
    private PingAccess() {
    }

    public static boolean canTargetOther(boolean playerSource, boolean othersPermission) {
        return !playerSource || othersPermission;
    }

    public static boolean canUseSelf(boolean playerSource) {
        return playerSource;
    }

    public static boolean targetAvailable(boolean online, boolean visible) {
        return online && visible;
    }
}

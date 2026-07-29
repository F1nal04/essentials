package f1nal.essentials.spawn;

/** Pure lifecycle-routing decisions for first joins and death respawns. */
public final class SpawnRouting {
    private SpawnRouting() {
    }

    public static boolean shouldRouteFirstJoin(
            boolean enabled, boolean firstJoin, boolean spawnSet) {
        return enabled && firstJoin && spawnSet;
    }

    public static boolean shouldRouteRespawn(boolean enabled, boolean alive,
            boolean spawnSet, boolean personalRespawnSet) {
        return enabled && !alive && spawnSet && !personalRespawnSet;
    }
}

package f1nal.essentials.spawn;

/** Minecraft-independent persisted Essentials spawn coordinates. */
public record SpawnLocation(String dimension, double x, double y, double z,
        float yaw, float pitch) {

    public SpawnLocation {
        if (dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("dimension must not be blank");
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("spawn coordinates must be finite");
        }
    }
}

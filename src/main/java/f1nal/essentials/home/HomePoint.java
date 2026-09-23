package f1nal.essentials.home;

/** Minecraft-independent saved home coordinates. */
public record HomePoint(String dimension, double x, double y, double z, float yaw, float pitch) {
    public HomePoint {
        if (dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("dimension must not be blank");
        }
        dimension = dimension.trim();
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("home coordinates must be finite");
        }
    }
}

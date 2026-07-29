package f1nal.essentials.spawn;

import java.util.Optional;

/**
 * Searches the configured point first, then nearby vertical and horizontal
 * positions. Safety evaluation is supplied by the Minecraft-facing adapter.
 */
public final class SafePositionFinder {
    static final int HORIZONTAL_RADIUS = 4;
    static final int VERTICAL_RADIUS = 8;

    private SafePositionFinder() {
    }

    public static Optional<Position> find(double x, double y, double z, Safety safety) {
        Position configured = new Position(x, y, z);
        if (safety.isSafe(configured)) return Optional.of(configured);
        double searchBaseY = Math.floor(y);
        for (int verticalOffset : verticalOffsets()) {
            Position candidate = new Position(x, searchBaseY + verticalOffset, z);
            if (safety.isSafe(candidate)) return Optional.of(candidate);
        }
        for (int radius = 1; radius <= HORIZONTAL_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    double candidateX = Math.floor(x) + dx + 0.5;
                    double candidateZ = Math.floor(z) + dz + 0.5;
                    for (int verticalOffset : verticalOffsets()) {
                        Position candidate = new Position(
                                candidateX, searchBaseY + verticalOffset, candidateZ);
                        if (safety.isSafe(candidate)) return Optional.of(candidate);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static int[] verticalOffsets() {
        int[] offsets = new int[VERTICAL_RADIUS * 2 + 1];
        offsets[0] = 0;
        for (int distance = 1; distance <= VERTICAL_RADIUS; distance++) {
            offsets[distance * 2 - 1] = distance;
            offsets[distance * 2] = -distance;
        }
        return offsets;
    }

    @FunctionalInterface
    public interface Safety {
        boolean isSafe(Position position);
    }

    public record Position(double x, double y, double z) {
    }
}

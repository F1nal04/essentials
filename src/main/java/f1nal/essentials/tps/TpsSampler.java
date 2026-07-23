package f1nal.essentials.tps;

import java.util.Optional;

/**
 * Bounded rolling tick-interval sampler. The sampler only stores primitive
 * durations and performs no work outside the calling thread.
 */
public final class TpsSampler {

    public static final int[] WINDOWS_SECONDS = {5, 10, 60, 300, 900};
    private static final int MAX_SAMPLES = 20 * WINDOWS_SECONDS[WINDOWS_SECONDS.length - 1];
    private static final long NANOS_PER_SECOND = 1_000_000_000L;

    private final long[] intervals = new long[MAX_SAMPLES];
    private int nextIndex;
    private int size;
    private long lastTickNanos = Long.MIN_VALUE;

    public synchronized void recordTick(long nowNanos) {
        if (lastTickNanos != Long.MIN_VALUE) {
            long elapsed = nowNanos - lastTickNanos;
            if (elapsed > 0) {
                addInterval(elapsed);
            }
        }
        lastTickNanos = nowNanos;
    }

    public synchronized void reset() {
        nextIndex = 0;
        size = 0;
        lastTickNanos = Long.MIN_VALUE;
    }

    /** Adds a measured interval directly, primarily for pure-JVM tests. */
    void addInterval(long durationNanos) {
        if (durationNanos <= 0) {
            throw new IllegalArgumentException("Tick duration must be positive");
        }
        intervals[nextIndex] = durationNanos;
        nextIndex = (nextIndex + 1) % intervals.length;
        if (size < intervals.length) {
            size++;
        }
    }

    public synchronized Optional<TpsSnapshot> snapshot() {
        if (size == 0) {
            return Optional.empty();
        }

        double[] values = new double[WINDOWS_SECONDS.length];
        int sampleCount = 0;
        long elapsedNanos = 0;
        int windowIndex = 0;

        for (int offset = 0; offset < size && windowIndex < WINDOWS_SECONDS.length; offset++) {
            int index = Math.floorMod(nextIndex - 1 - offset, intervals.length);
            elapsedNanos += intervals[index];
            sampleCount++;

            while (windowIndex < WINDOWS_SECONDS.length
                    && (elapsedNanos >= windowNanos(windowIndex) || sampleCount == size)) {
                values[windowIndex] = sampleCount * (double) NANOS_PER_SECOND / elapsedNanos;
                windowIndex++;
            }
        }

        return Optional.of(new TpsSnapshot(values));
    }

    private static long windowNanos(int index) {
        return WINDOWS_SECONDS[index] * NANOS_PER_SECOND;
    }
}

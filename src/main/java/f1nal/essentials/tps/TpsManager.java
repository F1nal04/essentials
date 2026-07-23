package f1nal.essentials.tps;

import java.util.Optional;

public final class TpsManager {

    private static final TpsSampler SAMPLER = new TpsSampler();
    private static boolean running;

    private TpsManager() {
    }

    public static void start() {
        SAMPLER.reset();
        running = true;
    }

    public static void recordTick(long nowNanos) {
        if (running) {
            SAMPLER.recordTick(nowNanos);
        }
    }

    public static Optional<TpsSnapshot> snapshot() {
        return SAMPLER.snapshot();
    }

    public static void stop() {
        running = false;
        SAMPLER.reset();
    }
}

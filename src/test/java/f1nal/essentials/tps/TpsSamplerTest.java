package f1nal.essentials.tps;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TpsSamplerTest {

    @Test
    void calculatesAllFiveRollingWindows() {
        TpsSampler sampler = new TpsSampler();
        add(sampler, 8_000, 50_000_000L);
        add(sampler, 5_000, 60_000_000L);
        add(sampler, 4_000, 75_000_000L);
        add(sampler, 1_000, 100_000_000L);

        double[] actual = sampler.snapshot().orElseThrow().values();
        assertArrayEquals(expected(samplerDurations()), actual, 0.000_001);
    }

    @Test
    void recentStartupUsesEveryAvailableSampleForLongerWindows() {
        TpsSampler sampler = new TpsSampler();
        add(sampler, 20, 50_000_000L);

        assertArrayEquals(new double[] {20, 20, 20, 20, 20},
                sampler.snapshot().orElseThrow().values(), 0.000_001);
    }

    @Test
    void firstTickOnlyEstablishesTheTimingBaseline() {
        TpsSampler sampler = new TpsSampler();
        sampler.recordTick(1_000_000_000L);
        assertTrue(sampler.snapshot().isEmpty());

        sampler.recordTick(1_050_000_000L);
        assertArrayEquals(new double[] {20, 20, 20, 20, 20},
                sampler.snapshot().orElseThrow().values(), 0.000_001);
    }

    private static void add(TpsSampler sampler, int count, long duration) {
        for (int i = 0; i < count; i++) {
            sampler.addInterval(duration);
        }
    }

    private static long[] samplerDurations() {
        long[] durations = new long[18_000];
        java.util.Arrays.fill(durations, 0, 8_000, 50_000_000L);
        java.util.Arrays.fill(durations, 8_000, 13_000, 60_000_000L);
        java.util.Arrays.fill(durations, 13_000, 17_000, 75_000_000L);
        java.util.Arrays.fill(durations, 17_000, 18_000, 100_000_000L);
        return durations;
    }

    private static double[] expected(long[] oldestToNewest) {
        double[] expected = new double[TpsSampler.WINDOWS_SECONDS.length];
        for (int window = 0; window < TpsSampler.WINDOWS_SECONDS.length; window++) {
            long threshold = TpsSampler.WINDOWS_SECONDS[window] * 1_000_000_000L;
            long elapsed = 0;
            int count = 0;
            for (int i = oldestToNewest.length - 1; i >= 0; i--) {
                elapsed += oldestToNewest[i];
                count++;
                if (elapsed >= threshold) {
                    break;
                }
            }
            expected[window] = count * 1_000_000_000.0 / elapsed;
        }
        return expected;
    }
}

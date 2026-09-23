package f1nal.essentials.home;

import java.util.function.IntPredicate;

/**
 * Resolves how many homes a player may keep. Numeric permissions replace the
 * configured default; the highest granted number that does not exceed the
 * configured maximum wins. Unlimited bypasses both.
 */
public final class HomeLimits {
    public static final int UNLIMITED = Integer.MAX_VALUE;

    private HomeLimits() {
    }

    public static int resolve(int defaultLimit, int maximumLimit, boolean unlimited,
            IntPredicate grantedLimit) {
        if (unlimited) return UNLIMITED;
        int ceiling = Math.max(0, maximumLimit);
        int highest = -1;
        for (int number = 1; number <= ceiling; number++) {
            if (grantedLimit.test(number)) highest = number;
        }
        int chosen = highest >= 0 ? highest : defaultLimit;
        if (chosen < 0) chosen = 0;
        return Math.min(chosen, ceiling);
    }

    public static boolean canCreate(int currentCount, int limit) {
        if (currentCount < 0) return false;
        return limit == UNLIMITED || currentCount < limit;
    }
}

package f1nal.essentials.tps;

import java.util.Locale;

public final class TpsDisplay {

    public static final String HEADER = "TPS from last 5s, 10s, 1m, 5m, 15m:";

    private TpsDisplay() {
    }

    public static Reading reading(double rawTps, double targetTps,
            double healthyThreshold, double degradedThreshold) {
        double displayed = Math.min(rawTps, targetTps);
        Health health = displayed >= healthyThreshold
                ? Health.HEALTHY
                : displayed >= degradedThreshold ? Health.DEGRADED : Health.CRITICAL;
        return new Reading(displayed, rawTps > targetTps, health);
    }

    public enum Health {
        HEALTHY,
        DEGRADED,
        CRITICAL
    }

    public record Reading(double value, boolean capped, Health health) {

        public String text() {
            return (capped ? "*" : "") + String.format(Locale.ROOT, "%.1f", value);
        }
    }
}

package f1nal.essentials.tps;

public record TpsSnapshot(double[] values) {

    public TpsSnapshot {
        if (values.length != TpsSampler.WINDOWS_SECONDS.length) {
            throw new IllegalArgumentException("A TPS snapshot must contain all rolling windows");
        }
        values = values.clone();
    }

    @Override
    public double[] values() {
        return values.clone();
    }
}

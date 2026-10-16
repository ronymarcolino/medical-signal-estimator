package com.pulsemotion.app;

import java.util.ArrayDeque;

/** Experimental periodic-motion detector; periodicity does not prove a cardiac origin. */
public final class HeartRateEstimator {
    private static final double HZ = 100.0;
    private final ArrayDeque<double[]> samples = new ArrayDeque<>();
    private double lastTime = -1;
    public record Result(int bpm, double quality, String status) {}

    public void reset() { samples.clear(); lastTime = -1; }

    public void add(double seconds, double x, double y, double z) {
        if (lastTime >= 0 && (seconds <= lastTime || seconds - lastTime > .25)) reset();
        lastTime = seconds;
        samples.add(new double[]{seconds, x, y, z});
        while (!samples.isEmpty() && seconds - samples.peekFirst()[0] > 20) samples.removeFirst();
    }

    public Result estimate() {
        if (samples.size() < 2) return new Result(0, 0, "Waiting for sensor");
        double[][] raw = samples.toArray(new double[0][]);
        double duration = raw[raw.length - 1][0] - raw[0][0];
        if (duration < 15) return new Result(0, 0, "Collecting · " + (int) duration + " / 15 s");
        if (raw.length / duration < 45) return new Result(0, 0, "Sensor sampling too slowly");
        int n = (int) (duration * HZ);
        double[][] signal = new double[3][n];
        int j = 0;
        double motion = 0;
        for (int i = 0; i < n; i++) {
            double t = raw[0][0] + i / HZ;
            while (j + 1 < raw.length - 1 && raw[j + 1][0] < t) j++;
            double mix = (t - raw[j][0]) / (raw[j + 1][0] - raw[j][0]);
            for (int axis = 0; axis < 3; axis++) signal[axis][i] = raw[j][axis + 1] * (1 - mix) + raw[j + 1][axis + 1] * mix;
        }
        double best = 0;
        int bpm = 0;
        for (double[] axis : signal) {
            double mean = 0;
            for (double v : axis) mean += v / n;
            double variance = 0;
            for (double v : axis) variance += (v - mean) * (v - mean) / n;
            motion += variance;
            // Time-domain high-pass (~0.7 Hz), then low-pass (~8 Hz).
            double baseline = axis[0], smooth = 0;
            double highAlpha = 1 - Math.exp(-2 * Math.PI * .7 / HZ);
            double lowAlpha = 1 - Math.exp(-2 * Math.PI * 8 / HZ);
            for (int i = 0; i < n; i++) {
                baseline += highAlpha * (axis[i] - baseline);
                smooth += lowAlpha * (axis[i] - baseline - smooth);
                axis[i] = smooth;
            }
            double[] corr = new double[151];
            for (int lag = 33; lag <= 150; lag++) {
                double dot = 0, a = 0, b = 0;
                for (int i = 200 + lag; i < n; i++) {
                    dot += axis[i] * axis[i - lag];
                    a += axis[i] * axis[i]; b += axis[i - lag] * axis[i - lag];
                }
                corr[lag] = a > 1e-7 && b > 1e-7 ? dot / Math.sqrt(a * b) : 0;
            }
            // Prefer the first strong local peak to reduce doubled-period selection.
            double max = 0;
            for (int lag = 34; lag < 150; lag++) if (corr[lag] >= corr[lag - 1] && corr[lag] > corr[lag + 1]) max = Math.max(max, corr[lag]);
            for (int lag = 34; lag < 150; lag++) {
                if (corr[lag] >= .65 && corr[lag] >= max * .92 && corr[lag] >= corr[lag - 1] && corr[lag] > corr[lag + 1]) {
                    if (corr[lag] > best) { best = corr[lag]; bpm = (int) Math.round(60 * HZ / lag); }
                    break;
                }
            }
        }
        if (Math.sqrt(motion) > .65) return new Result(0, 0, "Too much movement · hold still");
        if (bpm == 0) return new Result(0, best, "Weak signal · adjust phone position");
        return new Result(bpm, best, "Periodic signal · experimental estimate");
    }
}

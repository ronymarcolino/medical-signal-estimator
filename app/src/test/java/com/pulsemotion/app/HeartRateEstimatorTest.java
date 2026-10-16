package com.pulsemotion.app;
import org.junit.Test;
import static org.junit.Assert.*;

public class HeartRateEstimatorTest {
    @Test public void recoversSyntheticPulseWithGravityAndBreathing() {
        HeartRateEstimator estimator = new HeartRateEstimator();
        for (int i = 0; i <= 2000; i++) {
            double t = i / 100.0;
            estimator.add(t, 0, 0, 9.81 + .025 * Math.sin(2 * Math.PI * 1.2 * t) + .008 * Math.sin(2 * Math.PI * .2 * t));
        }
        assertEquals(72, estimator.estimate().bpm(), 2);
    }
    @Test public void rejectsStillness() {
        HeartRateEstimator estimator = new HeartRateEstimator();
        for (int i = 0; i <= 2000; i++) estimator.add(i / 100.0, 0, 0, 9.81);
        assertEquals(0, estimator.estimate().bpm());
    }
    @Test public void rejectsLargePeriodicMovement() {
        HeartRateEstimator estimator = new HeartRateEstimator();
        for (int i = 0; i <= 2000; i++) estimator.add(i / 100.0, 0, 0, 9.81 + 2 * Math.sin(2 * Math.PI * 1.2 * i / 100));
        assertEquals(0, estimator.estimate().bpm());
    }
    @Test public void resetsAfterSensorGap() {
        HeartRateEstimator estimator = new HeartRateEstimator();
        for (int i = 0; i <= 2000; i++) estimator.add(i / 100.0, 0, 0, 9.81);
        estimator.add(22, 0, 0, 9.81);
        assertEquals("Waiting for sensor", estimator.estimate().status());
    }
}

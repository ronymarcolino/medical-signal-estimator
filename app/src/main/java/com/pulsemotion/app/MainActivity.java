package com.pulsemotion.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.content.Context;
import android.hardware.*;
import android.view.*;
import android.widget.*;
import java.util.Locale;

public class MainActivity extends Activity implements SensorEventListener {
    private final int ink = Color.rgb(232, 239, 244), muted = Color.rgb(149, 167, 182), mint = Color.rgb(98, 231, 185);
    private SensorManager sensors;
    private Sensor accelerometer;
    private final HeartRateEstimator estimator = new HeartRateEstimator();
    private TextView value, status, axes, quality;
    private Button start;
    private Waveform waveform;
    private boolean running;
    private long lastUi, lastEstimate;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(11, 20, 29));
        getWindow().setNavigationBarColor(Color.rgb(11, 20, 29));
        sensors = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(11, 20, 29));
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(24), dp(28), dp(24), dp(28)); scroll.addView(root);
        root.addView(label("PULSE / MOTION", 14, mint));
        TextView title = label("Listen to your rhythm", 30, ink); title.setPadding(0, dp(22), 0, dp(8)); root.addView(title);
        root.addView(label("Heart-rate exploration through motion", 15, muted));
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setGravity(Gravity.CENTER); card.setPadding(dp(18), dp(22), dp(18), dp(22));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.rgb(20, 35, 47)); bg.setCornerRadius(dp(24)); card.setBackground(bg);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2); cp.setMargins(0, dp(28), 0, dp(20)); root.addView(card, cp);
        card.addView(label("EXPERIMENTAL HEART RATE", 12, muted));
        value = label("—", 76, ink); card.addView(value); card.addView(label("beats per minute", 14, muted));
        status = label("Ready when you are", 14, mint); status.setGravity(Gravity.CENTER); status.setPadding(0, dp(18), 0, 0); card.addView(status);
        waveform = new Waveform(this); root.addView(waveform, new LinearLayout.LayoutParams(-1, dp(140)));
        quality = label("SIGNAL QUALITY  ·  —", 12, mint); root.addView(quality);
        axes = label("X  —    Y  —    Z  —  m/s²", 12, muted); axes.setPadding(0, dp(12), 0, dp(22)); root.addView(axes);
        start = new Button(this); start.setText("Start measurement"); start.setTextColor(Color.rgb(11, 20, 29)); start.setBackgroundTintList(android.content.res.ColorStateList.valueOf(mint)); root.addView(start, new LinearLayout.LayoutParams(-1, dp(56)));
        start.setOnClickListener(v -> { if (running) stop("Measurement stopped"); else begin(); });
        TextView how = label("Make room for stillness", 20, ink); how.setPadding(0, dp(28), 0, dp(10)); root.addView(how);
        root.addView(label("1   Recline and rest the phone flat on your chest.\n\n2   Stay still and breathe normally.\n\n3   Wait 15–20 seconds for a periodic signal.", 15, muted));
        TextView note = label("Experimental · Not medically validated\nMotion can resemble a heartbeat. Estimates may be incorrect, even with a strong signal. Do not use for diagnosis or treatment. Compare with a validated monitor.\n\nPrivate by design: readings stay in memory on this device and are cleared when stopped.", 13, muted); note.setPadding(0, dp(26), 0, 0); root.addView(note);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(scroll);
        if (accelerometer == null) { status.setText("No accelerometer available"); start.setEnabled(false); }
    }
    private void begin() {
        estimator.reset(); waveform.clear(); lastEstimate = 0; lastUi = 0; value.setText("—"); quality.setText("SIGNAL QUALITY  ·  —");
        running = sensors.registerListener(this, accelerometer, 10000);
        if (!running) { status.setText("Unable to start accelerometer"); return; }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        start.setText("Stop measurement"); status.setText("Collecting · stay still");
    }
    private void stop(String message) {
        sensors.unregisterListener(this); running = false; estimator.reset(); waveform.clear();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        start.setText("Start measurement"); status.setText(message); value.setText("—"); quality.setText("SIGNAL QUALITY  ·  —"); axes.setText("X  —    Y  —    Z  —  m/s²");
    }
    @Override protected void onPause() { super.onPause(); if (running) stop("Paused · start a new measurement"); }
    @Override public void onSensorChanged(SensorEvent e) {
        if (!running) return;
        estimator.add(e.timestamp / 1e9, e.values[0], e.values[1], e.values[2]);
        if (e.timestamp - lastUi > 50000000L) {
            lastUi = e.timestamp;
            axes.setText(String.format(Locale.US, "X  %.2f    Y  %.2f    Z  %.2f  m/s²", e.values[0], e.values[1], e.values[2]));
            waveform.add(e.values[2]);
        }
        if (e.timestamp - lastEstimate > 1000000000L) {
            lastEstimate = e.timestamp;
            HeartRateEstimator.Result r = estimator.estimate();
            value.setText(r.bpm() == 0 ? "—" : Integer.toString(r.bpm())); status.setText(r.status());
            quality.setText(r.bpm() == 0 ? "SIGNAL QUALITY  ·  INSUFFICIENT" : "SIGNAL QUALITY  ·  PERIODIC " + (int)(r.quality() * 100) + "%");
        }
    }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density); }
    private TextView label(String text, int size, int color) { TextView v = new TextView(this); v.setText(text); v.setTextSize(size); v.setTextColor(color); v.setFontFeatureSettings("tnum"); return v; }

    private class Waveform extends View {
        private final float[] points = new float[160]; private int count, cursor; private float baseline;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Waveform(Context c) { super(c); }
        void clear() { count = cursor = 0; baseline = 0; invalidate(); }
        void add(float v) { if (count == 0) baseline = v; baseline += .12f * (v - baseline); points[cursor] = v - baseline; cursor = (cursor + 1) % points.length; count = Math.min(count + 1, points.length); invalidate(); }
        @Override protected void onDraw(Canvas c) {
            paint.setStrokeWidth(dp(1)); paint.setColor(Color.rgb(30, 47, 59));
            for (int i = 1; i < 4; i++) c.drawLine(0, getHeight()*i/4f, getWidth(), getHeight()*i/4f, paint);
            float peak = .03f; for (int i = 0; i < count; i++) peak = Math.max(peak, Math.abs(points[i]));
            Path p = new Path();
            for (int i = 0; i < count; i++) { int index = (cursor - count + i + points.length) % points.length; float x = i * getWidth() / 159f, y = getHeight()/2f - points[index]/peak * getHeight()*.4f; if (i == 0) p.moveTo(x,y); else p.lineTo(x,y); }
            paint.setColor(mint); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(2)); c.drawPath(p,paint); paint.setStyle(Paint.Style.FILL);
        }
    }
}

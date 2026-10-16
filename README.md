# Pulse Motion

Native Java Android app that displays live accelerometer axes and a motion waveform, then estimates the dominant periodic rate from a rolling 20-second window. No network, account, runtime permissions, or stored measurements.

## Build and run

Open this folder in Android Studio with Android SDK 35 and JDK 17. Use Gradle 8.9 (no Gradle wrapper is bundled). Sync, then run the `app` configuration on a physical Android 8.0+ device. With Gradle 8.9 installed, `gradle :app:assembleDebug :app:testDebugUnitTest` builds the APK and runs synthetic estimator tests. APK output: `app/build/outputs/apk/debug/app-debug.apk`.

This workspace also has portable build tools under the ignored `.tooling/` folder. Run `powershell -ExecutionPolicy Bypass -File .\build-local.ps1` to build and test, or add `-Install` to install on the single USB-authorized Android device. The script uses the already downloaded tools; `.tooling/` is not bundled with the source.

## Measurement

Recline, rest the phone flat on the chest, tap Start, and remain still while breathing normally. The app requests 100 Hz accelerometer readings and starts estimating after 15 seconds. Leaving the app stops acquisition and clears readings. Devices without an accelerometer show an unavailable state.

Timestamped samples are interpolated to 100 Hz, filtered (~0.7–8 Hz), and compared using normalized autocorrelation on each axis. The detector searches approximately 40–176 bpm and rejects low periodicity, large movement, gaps, and sampling below 45 Hz. The waveform displays baseline-removed Z acceleration with automatic scaling. The quality percentage measures periodicity, **not confidence in medical accuracy**.

This is an experimental periodic-motion detector, not a validated heart-rate monitor. Breathing harmonics, vibration, and repeated movement can produce false estimates. Thresholds are heuristic; synthetic tests do not establish accuracy on people. Validation against synchronized ECG or a validated monitor across devices and users is required before relying on results. Do not use for diagnosis or treatment.

## References

- Android accelerometer API and sensor lifecycle: https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion
- Sensor availability and sampling restrictions: https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview

## Validation in this workspace

On October 15, 2026, portable JDK 17, Gradle 8.9, and Android SDK tools were downloaded into `.tooling/`. The debug APK compiled successfully and all four estimator unit tests passed. ADB installed and launched the app successfully on a Samsung Galaxy A25 (SM-A256E), Android API 36. A brief physical-device smoke test confirmed live accelerometer axes, waveform updates, and the weak-signal state without a BPM estimate. This verifies app operation, not heart-rate accuracy; no reference-monitor comparison has been performed.

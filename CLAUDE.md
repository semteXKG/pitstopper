# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

PitStopper is an Android app for endurance racing pit window timing. It displays a large countdown timer, flashes visual alerts when pit windows open, and uses GPS to auto-detect pit stops. It also integrates with MyLaps SpeedHive for live timing data and uses MQTT for inter-device communication (chat, telemetry, flash messages).

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires keystore.properties with signing config)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "at.semmal.pitstopper.timing.PitWindowAlertManagerTest"

# Clean build
./gradlew clean
```

Release APKs go to `releases/pitstopper-X.X.X.apk`. Version numbers are in `app/build.gradle.kts` (`versionCode` and `versionName`).

## Architecture

**Language**: Java 11 (no Kotlin source code). Gradle build scripts are Kotlin DSL.

**Package**: `at.semmal.pitstopper`

### Key Packages

- **`activities/`** - Android Activities. `MainActivity` is the main screen; `SessionActivity` handles MQTT session creation/joining via QR codes. Settings are split across `SettingsActivity` (hub), `SettingsRaceActivity`, `SettingsSpeedHiveActivity`, `SettingsMqttActivity`, `SettingsTelemetryActivity`.
- **`ui/`** - Modular UI components that plug into `MainActivity`. Each `*Module` class manages a section of the main screen: `PitTimerModule` (pit window countdown/flash), `CountdownModule`, `CenterModule`, `TelemetryModule`, `ChatModule`, `TroubleshootModule`, `CustomModule`. Modules are swappable via swipe gestures handled by `SwipeInterceptLayout`.
- **`timing/`** - Pit window logic. `PitWindowAlertManager` tracks alert state (IDLE/ON_ALERT) with window suppression when GPS detects a stop. `PitWindowPreferences` persists settings via SharedPreferences.
- **`mqtt/`** - MQTT messaging layer. `MqttClientManager` handles HiveMQ MQTT connections. `ExternalSessionManager` manages session topics. `LocalTcpProxy` routes MQTT through cellular to bypass car WiFi DNS issues. `WifiNetworkManager` handles WiFi SSID detection and network binding.
- **`livetiming/`** - SpeedHive integration. `SpeedHiveManager` fetches live timing from the SpeedHive API (routed through cellular). `DemoSpeedHiveManager` provides simulated race data.
- **`gps/`** - `StandstillDetector` uses GPS to detect when the car has stopped in the pits.
- **`model/`** - Data classes: `LiveTimingData`, `SpeedHiveCar`, `SpeedHiveConfig`, `SpeedHiveEvent`, `SpeedHiveSession`, `TelemetryData`, `ChatMessage`.

### Application Singleton

`PitStopperApplication` extends `Application` and holds shared instances (e.g., `ExternalSessionManager`, `MqttClientManager`).

### Network Routing

The app runs on phones connected to car WiFi (which typically has no internet). External network calls (SpeedHive API, external MQTT) are routed through cellular via `LocalTcpProxy` and Android's network binding APIs in `WifiNetworkManager`.

### Deep Links

The app handles `pitstopper://join?session={uuid}` deep links for joining MQTT sessions.

## Testing

Unit tests are in `app/src/test/`. Currently only `PitWindowAlertManagerTest.java` exists with 42 tests covering pit window state machine logic. Run with `./gradlew test`.

## Release Process

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`
2. `./gradlew assembleRelease`
3. Copy APK to `releases/pitstopper-X.X.X.apk`
4. Git commit and tag with `vX.X.X` (never skip the tag)

## Key External Dependencies

- **HiveMQ MQTT Client** (`com.hivemq:hivemq-mqtt-client:1.3.3`) - MQTT messaging
- **Google Play Services Location** - GPS standstill detection
- **ZXing Core** (`com.google.zxing:core:3.5.3`) - QR code generation for session sharing
- **JSch** (`com.jcraft:jsch:0.1.55`) - SSH for network scanning

## Configuration Files

- `keystore.properties` - Release signing config (not committed secrets)
- `app/src/main/assets/speedhive.properties` - SpeedHive API credentials
- `local.properties` - Android SDK path (machine-specific)

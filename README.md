# AppPulse

Android SDK for crash analytics, performance monitoring, memory and leak detection, security scanning, and a unified health score. Collected data is stored locally in Room (`apppulse.db`).

```
AppPulse
├── apppulse-sdk    Android library (io.apppulse.sdk)
└── app             Sample consumer app (io.apppulse.sample)
```

## Installation

In the host app module:

```kotlin
dependencies {
    implementation(project(":apppulse-sdk"))
}
```

`settings.gradle.kts` must include the library:

```kotlin
include(":apppulse-sdk")
```

The library namespace is `io.apppulse.sdk`. Min SDK is 24.

## Initialization

Call `AppPulse.init` once from `Application.onCreate`:

```kotlin
class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppPulse.init(this)
    }
}
```

Optional configuration:

```kotlin
AppPulse.init(
    this,
    AppPulseConfig.Builder()
        .apiKey("your-api-key")
        .debugMode(true)
        .enableCrashTracking(true)
        .enablePerformanceTracking(true)
        .enableMemoryTracking(true)
        .enableSecurityTracking(true)
        .enableAutoSync(true)
        .syncIntervalMinutes(30)
        .build()
)
```

Register the application class in the host manifest with `android:name`.

## Basic usage

```kotlin
AppPulse.getHealthScore()
AppPulse.generateHealthReport()
AppPulse.openHealthDashboard(context)
AppPulse.openCrashDashboard(context)
AppPulse.runSecurityScan()
```

Other entry points:

```kotlin
AppPulse.openPerformanceDashboard(context)
AppPulse.openMemoryDashboard(context)
AppPulse.openSecurityDashboard(context)
AppPulse.getSecurityScore()
AppPulse.getMemoryHealthScore()
AppPulse.simulateActivityLeak(activity)
AppPulse.syncNow()
```

The sample app (`io.apppulse.sample`) wires these from `MainActivity`:

- Crash Test throws an uncaught exception so the crash handler can store it.
- ANR Test blocks the main thread for 6 seconds.
- Memory Leak Test retains an activity, then finishes it.
- Security Scan Test runs `AppPulse.runSecurityScan()` and opens the security dashboard.
- Open Health Dashboard reads the score, generates a report, and opens the health dashboard.

## Features

- Crash analytics and a local crash dashboard
- Performance monitoring (startup, screens, FPS, network, ANR) and a performance dashboard
- Memory monitoring, leak detection, large-bitmap tracking, and a memory dashboard
- Security scanner (manifest, permissions, cleartext traffic, WebView) and a security dashboard
- Unified health score and health report (JSON and CSV export)
- Local Room storage and optional remote sync

## Public API

These methods are public on `io.apppulse.sdk.AppPulse`:

- `AppPulse.init()`
- `AppPulse.getHealthScore()`
- `AppPulse.generateHealthReport()`
- `AppPulse.openCrashDashboard()`
- `AppPulse.openHealthDashboard()`
- `AppPulse.runSecurityScan()`

# StepSync Codebase Standards

## Architecture

StepSync is a single-module Android app written in Kotlin with XML layouts. Keep UI logic in activities/fragments, long-running tracking work in `StepTrackingService`, and shared rules in small utilities under `com.android.stepsync.utils`.

Use `StepSyncConfig` for shared preference names, Firebase URLs, broadcast actions, default values, and unit constants. Avoid repeating raw keys such as `step_sync_prefs`, `units`, or `com.android.stepsync.UNITS_CHANGED` in feature code.

Use `TrackingFormatters` for distance, speed, duration, pace, and step-estimation math. This keeps kilometers/miles behavior consistent across Home, Record, Settings, notifications, and activity history.

## Firebase

Client code must never store passwords, service account keys, admin SDK credentials, or private secrets in Realtime Database. Password updates must go through Firebase Auth.

Keep database writes scoped to the current authenticated user's UID. For new user data, prefer multi-path updates when multiple indexes need to stay consistent.

## Android Runtime

Request runtime permissions only when they apply to the current Android version. Use `ContextCompat.startForegroundService` for starting foreground services and keep notification updates lightweight.

Use `LocalBroadcastManager` for in-app tracking events. If a system broadcast is also required, set the package before sending it so it stays app-scoped.

## Code Style

Prefer named constants over repeated strings and magic numbers. Keep comments for non-obvious behavior, version compatibility, or security decisions; remove comments that simply restate the next line of code.

Avoid debug-only write paths in production code. Test helpers should live in tests or behind an explicit build-time mechanism.

Run these checks before opening a PR:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat lintDebug
.\gradlew.bat testDebugUnitTest
```

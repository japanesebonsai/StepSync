# StepSync

<p align="center">
  <img src="docs/assets/stepsync-logo.png" alt="StepSync logo" width="140" />
</p>

StepSync is an Android fitness tracking app for recording walking activity, tracking daily step goals, and reviewing weekly progress. It uses Firebase Authentication and Firebase Realtime Database for account and activity data.

<p align="center">
  <img src="docs/assets/stepsync-home.png" alt="StepSync home screen showing daily goal, current activity, and weekly stats" width="320" />
</p>

## Features

- Email/password sign up and login with Firebase Authentication
- Foreground step tracking service with pause, resume, and stop controls
- Daily step goal progress with configurable step length
- Distance, duration, speed, pace, and activity history
- Kilometers and miles support
- Weekly activity summary
- Profile editing with Firebase Auth password updates
- Firebase Realtime Database security rules for user-owned data

## Tech Stack

- Kotlin
- Android XML layouts
- AndroidX, Material Components, Navigation
- Firebase Authentication
- Firebase Realtime Database
- Gradle version catalog

## Setup

1. Open the project in Android Studio.
2. Use JDK 17 or the Android Studio bundled JDK.
3. Make sure `app/google-services.json` exists for the Firebase project.
4. Sync Gradle.
5. Run the app on an emulator or Android device.

Recommended emulator:

```text
Pixel 8 - API 35 - Google Play
```

## Firebase

Firebase config is handled by `app/google-services.json`. This file contains client app identifiers and the Firebase Android API key, not admin credentials.

Never commit:

- Firebase service account JSON
- Admin SDK private keys
- Release signing keys
- Keystore passwords
- `.env` files with private secrets

Realtime Database rules are configured in:

```text
rules/database-rules.json
```

Deploy rules with:

```powershell
npx.cmd firebase-tools deploy --only database
```

## Verification

Run these before opening or updating a PR:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat lintDebug
.\gradlew.bat testDebugUnitTest
```

## Documentation

- [Codebase standards](docs/codebase-standards.md)
- [Firebase CLI notes](docs/firebase-cli.md)

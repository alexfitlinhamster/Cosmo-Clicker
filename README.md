# Cosmo Clicker

Cosmo Clicker is an incremental Android game about collecting space debris, expanding a drone fleet, and travelling between increasingly valuable planets. The interface is built entirely with Jetpack Compose.

## Features

- Tap planets to collect debris and improve your click power.
- Build passive income with a fleet of animated scavenging drones.
- Unlock eight planets with different prices and income bonuses.
- React to random space storms, golden asteroids, and pirate attacks.
- Open cases to receive a random drone.
- Purchase upgrades and manage debt while growing the space operation.
- Keep progress between sessions with local Android storage.

## Tech stack

- Kotlin
- Jetpack Compose and Material 3
- MVVM with `ViewModel`, coroutines, and `StateFlow`
- Gradle Kotlin DSL
- Android SDK 24+

## Requirements

- Android Studio with JDK 11 or newer
- Android SDK 37
- An emulator or Android device running Android 7.0 (API 24) or newer

## Build and run

1. Clone the repository.
2. Open the project in Android Studio.
3. Let Gradle sync the project dependencies.
4. Select an emulator or connected Android device and run the `app` configuration.

To build a debug APK from the command line on Windows:

```powershell
.\gradlew.bat assembleDebug
```

The APK is generated in `app/build/outputs/apk/debug/`.

## Versioning

The current release uses two values defined in `gradle.properties`:

- `APP_VERSION_NAME` is the public, human-readable application version.
- `APP_VERSION_CODE` is the positive integer Android uses to order releases.

Increase `APP_VERSION_CODE` for every published build and update `APP_VERSION_NAME` when preparing a new release. Record the same values and a short English summary in [`news.md`](news.md).

## Release notes

See [`news.md`](news.md) for the version history and brief changes.

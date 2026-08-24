# Drona Salvage

Drona Salvage is a single-player incremental Android game about collecting space debris, expanding a drone fleet, and travelling between increasingly valuable planets. It works offline, stores progress locally, and uses a Jetpack Compose interface.

## Features

- Tap planets to collect debris and improve your click power.
- Build passive income with a fleet of animated scavenging drones, with separate
  owned and active-fleet limits protected when loading older saves.
- Follow a 39-planet galaxy route with rising prices, permanent gameplay bonuses,
  fleet requirements, drone-discovery goals, and prestige milestones.
- React to weighted, multi-stage space events including golden salvage, meteor
  showers, black holes, the Helios Protocol, pirate raids, trading ships, cyber
  viruses, and branching distress-signal and abandoned-station expeditions.
- Repair a cyber-virus infection through an interactive 3x3 terminal puzzle. Success
  grants debris; failure costs resources and temporarily disables the infected drone.
- Open Common, Rare, and Legendary cases with different prices and rarity odds.
  Every case has a stable eight-frame opening animation and tier-specific artwork.
- Purchase upgrades and manage debt while growing the space operation.
- Keep progress between sessions with local Android storage.
- Complete rotating daily and weekly quests, claim achievements, build permanent
  prestige upgrades, and earn offline fleet progress between sessions.
- Play in English, Russian, or Spanish, with reduced-motion and sound controls.

## Project structure

- `app/src/main/java/com/example/myapplication/` contains the game state, economy,
  event and progression engines, persistence repository, and ViewModel orchestration.
- `app/src/main/java/com/example/myapplication/ui/` contains the Compose screens,
  dialogs, navigation panels, reusable components, and theme.
- `app/src/main/res/` contains localized text, audio, icons, and game artwork.
- `app/src/test/` contains deterministic unit and regression tests; `app/src/androidTest/`
  contains the Compose launch, settings, language, and reset smoke tests.

## Tech stack

- Kotlin
- Jetpack Compose and Material 3
- MVVM with `ViewModel`, coroutines, and `StateFlow`
- Gradle Kotlin DSL
- Android SDK 24+

## Requirements

- Android Studio with JDK 17 or newer
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

Useful verification commands:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

The connected test task requires a running emulator or attached device.

## Versioning

The current release uses two values defined in `gradle.properties`:

- `APP_VERSION_NAME` is the public, human-readable application version.
- `APP_VERSION_CODE` is the positive integer Android uses to order releases.

Increase `APP_VERSION_CODE` for every published build and update `APP_VERSION_NAME` when preparing a new release. Record the same values and a short English summary in [`news.md`](news.md).

The current project version is **1.18.0 (37)**. Work completed after that release is
listed under **Unreleased** until the next version is assigned.

## Release notes

See [`news.md`](news.md) for the version history and brief changes.

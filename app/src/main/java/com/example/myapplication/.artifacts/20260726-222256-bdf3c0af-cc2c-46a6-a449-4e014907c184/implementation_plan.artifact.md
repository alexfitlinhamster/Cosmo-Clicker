# Implementation Plan - Codex Feature

Add a "Codex" system to track and display discovered planets, drones, and debris types.

## Proposed Changes

### Data Model

#### [GameState.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplication4/app/src/main/java/com/example/myapplication/GameState.kt)
- Add discovered sets to `GameState`:
```kotlin
val discoveredPlanets: Set<String> = setOf("p1"),
val discoveredDrones: Set<String> = emptySet(),
val discoveredDebris: Set<Int> = emptySet()
```

### Business Logic

#### [GameViewModel.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplication4/app/src/main/java/com/example/myapplication/GameViewModel.kt)
- **Discovery Logic**:
    - Update `buyPlanet`: Add planet to `discoveredPlanets`.
    - Update `finishOpeningCase`: Add drone type to `discoveredDrones`.
    - Update `claimQuestReward`: Add drone/case types to discovered sets.
    - Update `updateDrones`: When `nHasCargo` is true and drone returns home, add `nCargoImageIndex` to `discoveredDebris`.
- **Persistence**: Save/load discovered sets in `SharedPreferences`.

### UI Components

#### [NEW] [CodexPanel.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplication4/app/src/main/java/com/example/myapplication/ui/components/CodexPanel.kt)
- Create a slide-up panel with three tabs: **Planets**, **Fleet**, **Debris**.
- Items not discovered will show as "???" with a generic placeholder icon.
- Discovered items will show full details (name, rarity, image).

#### [GameScreen.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplication4/app/src/main/java/com/example/myapplication/ui/GameScreen.kt)
- Add `isCodexOpen` state.
- Add `CodexLauncherButton` to the bottom-left launcher column (alongside Quest and Shop).
- Integrate `CodexPanel` into the screen overlay.

### Resources

#### [strings.xml](file:///C:/Users/User/AndroidStudioProjects/MyApplication4/app/src/main/res/values/strings.xml)
- Add strings for Codex, Discovery tabs, and placeholder text.

## Verification Plan

### Manual Verification
1. **Discovery Flow**:
    - Collect a new type of debris and verify it appears in the Debris tab of the Codex.
    - Buy a new planet and verify it unlocks in the Planets tab.
    - Open a case to get a new drone and verify it unlocks in the Fleet tab.
2. **UI Check**:
    - Verify that locked items show as "???" and discovered items show full info.
    - Test the tabs switching inside the Codex panel.
3. **Persistence**:
    - Restart the app and verify all discovery progress is saved.

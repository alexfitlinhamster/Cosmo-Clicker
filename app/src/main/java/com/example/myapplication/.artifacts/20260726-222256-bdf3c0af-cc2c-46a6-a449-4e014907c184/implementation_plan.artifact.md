# Implementation Plan - New Debris and Enhanced Case System

Integrate 8 new debris types (debris_07 to debris_14) with rarities, automate case opening for quest rewards, and make the first shop case free.

## Proposed Changes

### Game Logic & Data

#### [GameViewModel.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplication4/app/src/main/java/com/example/myapplication/GameViewModel.kt)
- **New Debris Logic**:
    - Update `debrisImageIndex(rarity)` to include indices 7–14.
    - Rarities for new debris: 7-8 (Common), 9-10 (Uncommon), 11-12 (Rare), 13 (Epic), 14 (Legendary).
- **Free First Case**:
    - Update `calculateCaseCost` to return 0.0 if `casesPurchased == 0`.
- **Quest Reward Automation**:
    - Update `claimQuestReward` to trigger `startOpeningCase(isFree = true)` when a case reward is granted.
    - Ensure drone limit (5) is respected even for quest rewards.

#### [GameRules.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplication4/app/src/main/java/com/example/myapplication/GameRules.kt)
- Update `calculateCaseCost` logic to handle the first free case if applicable.

### UI Enhancements

#### [QuestPanel.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplication4/app/src/main/java/com/example/myapplication/ui/components/QuestPanel.kt)
- No changes needed to the panel itself, but ensure reward claiming triggers the case opening animation automatically in the main screen.

#### [GameScreen.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplication4/app/src/main/java/com/example/myapplication/ui/GameScreen.kt)
- The existing `CaseOpeningOverlay` will automatically show up when `isOpeningCase` is set to true by the automated quest reward.

## Verification Plan

### Automated Tests
- Build the project to ensure no syntax errors: `gradlew app:assembleDebug`

### Manual Verification
1. **Free Case**: Open the shop and verify the first "Mystery Case" cost is displayed as "FREE" (or 0).
2. **New Debris**: Play the game or trigger a debris shower to see `debris_07` through `debris_14` appear on screen.
3. **Quest Case Reward**: Complete a quest that rewards a case, click claim, and verify the case opening animation starts immediately.
4. **Drone Limit**: Reach 5 drones and verify quest cases are either blocked or handled (e.g., converted to debris) if the limit is reached.

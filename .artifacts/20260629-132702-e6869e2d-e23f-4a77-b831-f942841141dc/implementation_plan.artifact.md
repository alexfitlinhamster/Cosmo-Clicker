# Visual & Mechanics Overhaul

Improve the visual quality of planets, expand the planet roster based on the provided pixel art reference, and implement a more interactive drone scavenging mechanic.

## Proposed Changes

### Game Engine & State

#### [GameState.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplication4/app/src/main/java/com/example/myapplication/GameState.kt)
- Add `scavengingDrones` list to track active drone positions and states (idling, moving to debris, returning).
- Add `activeDebris` list for decorative debris that drones can target.

#### [GameViewModel.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplication4/app/src/main/java/com/example/myapplication/GameViewModel.kt)
- Expand `planets` map with new entries from the image (Mars, Volcania, etc.).
- Implement drone AI logic in a coroutine:
    - Periodically spawn a piece of "scavengable" debris.
    - Assign idle drones to move towards debris.
    - On arrival, wait a short time, then return to the planet.
    - On return, trigger a small debris gain.

### UI & Graphics

#### [MainActivity.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplication4/app/src/main/java/com/example/myapplication/MainActivity.kt)
- **Planet Rendering**: Move away from simple radial gradients. Use `Canvas` with multiple layers of noise, craters (circles with shadows), and atmospheric glows to simulate a "high-quality" look.
- **Drone Rendering**: Create a specific `@Composable` for drones that shows them carrying a small "debris" icon when returning.
- **Background**: Add a subtle parallax effect or a more layered starfield.

## Verification Plan

### Automated Tests
- No automated tests planned for this visual overhaul.

### Manual Verification
- Run the app and observe the new planet graphics.
- Buy a drone and verify it spawns, moves to debris, and returns to the planet.
- Verify that new planets (e.g., Volcania) appear in the shop and can be purchased.

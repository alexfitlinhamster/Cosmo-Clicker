# Implementation Plan: Asset Slicing and Integration

This plan outlines the steps to programmatically slice the provided sprite sheets into individual, high-quality PNG assets and integrate them into the Space Scavenger game.

## Proposed Changes

### 🛠️ Asset Processing Tool

I will create a Kotlin-based tool to slice the images. Since I cannot run Python reliably, I will use a JVM-based approach (using `java.awt.image`) within the project's test suite, which can be easily executed from Android Studio.

#### [NEW] [ImageSlicer.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplication4/app/src/test/java/com/example/myapplication/ImageSlicer.kt)

- **Purpose**: Programmatically slice `img.png` (planets) and `preview.png` (fleet).
- **Functionality**:
    - **Planets**: Slice into 4x3 grid. Apply a circular mask to remove square backgrounds and anti-alias the edges. Center each planet on a 512x512 transparent canvas.
    - **Fleet**: Slice into 6x5 grid (26 units). Trim excess transparency, remove labels, and center on 256x256 transparent canvases.
    - **Naming**: Save files according to the provided tables (e.g., `planet_01_earth.png`, `ast.png`).

---

### 🎮 Game Logic & UI

#### [GameViewModel.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplication4/app/src/main/java/com/example/myapplication/GameViewModel.kt)

- **Change**: Replace `SpriteInfo` with direct resource IDs (`R.drawable.planet_01_earth`, etc.).
- **Content**: Update `planets`, `fleetItems`, and `clickItems` to include all 12 planets and 26 fleet units correctly.

#### [MainActivity.kt](file:///C:/Users/User/AndroidStudioProjects/MyApplication4/app/src/main/java/com/example/myapplication/MainActivity.kt)

- **Change**: Remove the `SpriteImage` composable and its complex transformations.
- **Change**: Update `PlanetButton`, `ScavengingDrone`, `ShopRow`, and `PlanetRow` to use the standard `Image` composable with `painterResource`.
- **Polish**: Ensure consistent scaling and shadows are applied to the new assets.

---

## Verification Plan

### Automated Verification
- **Build**: Run `./gradlew assembleDebug` to ensure no resource or reference errors.
- **Asset Check**: Manually inspect the generated files in `app/src/main/res/drawable` for transparency and centering.

### Manual Verification
1. **Launch App**: Verify the starting planet (Earth) looks clean and circular.
2. **Shop Navigation**: Open the shop and check that all 12 planets and 26 fleet units have clear, high-quality icons.
3. **Purchasing**: Purchase different planets and fleet units to ensure they render correctly on the main screen.
4. **Visual Audit**: Check for "pixel leakage" or clipped edges on all 38 new assets.

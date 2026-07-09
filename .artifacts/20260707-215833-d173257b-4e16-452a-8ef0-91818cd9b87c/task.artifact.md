# Task Management

- [x] Research and Planning
    - [x] Analyze provided sprite sheets (`img.png` and `preview.png`)
    - [x] Define slicing coordinates and parameters for planets and fleet
- [x] Asset Slicing Implementation
    - [x] Create `ImageSlicer.kt` in the test source set to perform programmatic slicing
    - [x] Execute the slicer to generate 12 planet PNGs and 26 fleet PNGs in `res/drawable`
- [x] Game Logic and UI Integration
    - [x] Update `GameViewModel.kt` to reference individual drawable resources instead of sprite sheet coordinates
    - [x] Refactor `MainActivity.kt` to use standard `Image` components for planets and fleet units
    - [x] Ensure all 12 planets are correctly implemented and available in the shop
- [x] Verification and Polish
    - [x] Verify asset transparency and quality
    - [x] Test the game flow, including clicking, shop purchases, and drone movements
    - [x] Ensure visual consistency (lighting, scaling, centering)

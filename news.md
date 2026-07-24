# News

## 1.11 (version code 27) - 2026-07-24

- Added the missing localized names, descriptions, and bonuses for planets 16–20.
- Aligned asteroid and black-hole visuals with their logical game-area coordinates.
- Preserved late-game currency accurately with backward-compatible 64-bit saves.
- Reworked Hotel Debt into a 1M loan repaid by 30% of tap income without clearing the player balance.

## 1.10.6 (version code 26) - 2026-07-24

- Animated the start-screen prompt with the same floating fade style used by the case-opening text.

## 1.10.5 (version code 25) - 2026-07-24

- Added a localized tap-to-continue prompt to the bottom of the start screen.
- Made the prompt update automatically when the game language changes.

## 1.10.4 (version code 24) - 2026-07-22

- Restored 11 empty PNG resources from the original sprite sheets.
- Fixed the shop crash when rendering the Debris Harvester or Signal Beacon.
- Added an automated test that validates every drawable PNG can be decoded.

## 1.10.3 (version code 23) - 2026-07-22

- Increased the Mystery Case price by 20% after every purchase.
- Persisted the case purchase count across game restarts.
- Migrated existing saves using the number of currently owned drones.

## 1.10.2 (version code 22) - 2026-07-22

- Aligned planet, drone, and debris rendering to the same game-area coordinates.
- Made returning drones visibly reach the planet before delivering debris.
- Reduced the planet avoidance radius to better match its visible size.

## 1.10.1 (version code 21) - 2026-07-22

- Added a localized How to Play guide to the settings screen.
- Documented taps, debris rarity, drone collection, cases, events, meteors, and planet bonuses.

## 1.10 (version code 20) - 2026-07-22

- Added localized bonus descriptions below every planet in the shop.
- Added a 15% chance for Sylva taps to grant double income.
- Added the missing localized description for Mars.

## 1.9.1 (version code 19) - 2026-07-22

- Renamed the game to Orbit Salvagers: Drone Clicker.

## 1.9 (version code 18) - 2026-07-21

- Renamed drawable assets with consistent category prefixes.
- Updated static, dynamic, and image-slicing references to the new resource names.

## 1.8.1 (version code 17) - 2026-07-21

- Resized and cropped the meteor artwork to match the debris image canvas.
- Replaced the temporary meteor symbol with the meteor image during debris showers.

## 1.8 (version code 16) - 2026-07-21

- Added randomized currency rewards to every debris object.
- Increased rewards up to 5K for common, 20K for rare, 50K for epic, and 1M for legendary debris.
- Applied the legendary reward range to successfully collected meteors.

## 1.7 (version code 15) - 2026-07-21

- Added a 20% meteor spawn chance during debris showers.
- Added a 50% chance for collected meteors to disable a drone for one minute.
- Successful meteor collection now grants the legendary debris reward.

## 1.6 (version code 14) - 2026-07-20

- Replaced the collapsed shop bar with a bottom-left shop image button.
- Added lock artwork to unowned planets and click upgrades until their first purchase.

## 1.5.2 (version code 13) - 2026-07-20

- Moved rarity names and the case opening prompt to localized resources.
- Added English, Russian, and Spanish translations for the migrated text.

## 1.5.1 (version code 12) - 2026-07-20

- Added a compact app name, version, and build code caption to the settings header.

## 1.5 (version code 11) - 2026-07-20

- Made debris shower objects collectible by drones.
- Replaced debris placeholders with the musor1 through musor6 artwork.
- Added random drone patrols that avoid flying through the planet.

## 1.4 (version code 10) - 2026-07-20

- Added a debris shower event with animated flying space junk.
- Randomized every event duration between 20 and 60 seconds.

## 1.3.1 (version code 9) - 2026-07-20

- Fixed crashes caused by rapidly pressing any store action.
- Made purchases, sales, planet unlocks, cases, and debt actions atomic.

## 1.3 (version code 8) - 2026-07-20

- Added a short sound effect when the player taps a planet.
- Reused and safely released the audio generator across rapid taps.

## 1.2.3 (version code 7) - 2026-07-17

- Aligned the currency amount and label on a shared text baseline.
- Stabilized the currency counter layout with monospaced digits.

## 1.2.2 (version code 6) - 2026-07-17

- Fixed a crash when rapidly purchasing click upgrades.
- Made click upgrade purchases atomic and protected cost calculations from overflow.

## 1.2.1 (version code 5) - 2026-07-17

- Fixed debris and drones rendering in different coordinate spaces.
- Made drones reach debris before collecting it.
- Prevented multiple drones from targeting the same debris.

## 1.2 (version code 4) - 2026-07-17

- Added five debris rarity tiers with increasing rewards.
- Made drone rarity determine which debris tiers it can collect.
- Removed drone-based income so rewards now come from collected debris only.
- Added rarity-colored debris visuals and drone rarity details in the fleet shop.

## 1.1.1 (version code 3) - 2026-07-17

- Fixed a crash caused by rapid taps on the planet.
- Limited simultaneous floating reward animations to improve stability.

## 1.1 (version code 2) - 2026-07-17

### Version changes

- Added an in-game settings screen.
- Added application version, language, and developer information.
- Added persistent system, English, and Russian language selection.

### Code changes

- Added Spanish resources and Spanish language selection.
- Applied English, Russian, and Spanish resources throughout the game interface.

## 1.0 (version code 1) - 2026-07-17

- Added animated case opening with random drone rewards.
- Added new planet, drone, and item artwork.
- Added application versioning and English project documentation.

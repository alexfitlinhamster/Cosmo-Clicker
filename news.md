# News

## 1.16.0 — Cosmic Shop Update - 2026-08-10

- Fixed drones becoming permanently trapped or disabled during Black Hole events; they now keep collecting moving debris while the singularity affects the salvage field.
- Fixed case rewards surviving app restarts, made every dropped duplicate count as a real warehouse drone, removed the artificial ten-drone storage cap, and prevented reward screens from closing through background taps.
- Renamed the Case Shop to the Cosmic Shop to reflect its expanded selection of cases, planets, upgrades, and ship systems.
- Added expensive system upgrades for up to five drones in flight, faster debris spawning, and an expanding automatic salvage-magnet radius; corrected the hangar warehouse count display.
- Kept the simple single-drone reward screen for one case and reserved the animated card summary for bundles of two or more.
- Reduced drone resale payouts by about 1.5× to prevent bulk case rewards from creating a profitable sell-back loop.
- Shifted case drop tables toward common and uncommon drones, making epic and legendary rewards less frequent while preserving better odds in higher-tier cases.
- Added duplicate protection within case bundles so batches produce more different drone models before repeating the same model in each rarity tier.
- Added an Open All option for case bundles and a polished result screen where unique drone cards appear one by one with their total quantities.
- Added flexible case bundles up to the player's affordable maximum. Case prices now increase only after every 20 opened cases, and bundles open sequentially with a cosmic remaining-case counter.
- Refined the case quantity dialog: selection now starts at one and uses a compact, centered slider instead of uneven increment buttons.
- Improved the shop and drone warehouse with a visible purchase balance, clear capacity bars, larger drone cards, and a left-right case shake during opening.
- Reworked the event roster while preserving the Trading Ship and Black Hole:
  Golden Asteroids now require seven moving hits, and Space Storm and Solar Flare
  are active tap challenges with visible objectives and scaled rewards.
- Kept Meteor Shower, Cyber Virus, Distress Signal, Abandoned Station, and Pirate
  Raid as distinct interactive encounters with clearer instructions and artwork.
- Added six cohesive transparent event sprites for storm nodes, molten meteors,
  solar cooling equipment, cyber-virus modules, abandoned stations, and pirate ships.
- Replaced the detailed legacy app artwork with a cleaner salvage-drone emblem and
  rebuilt the Shop, Quests, Hangar, Achievements, Statistics, and Settings icons as
  one consistent navigation set.
- Reduced navigation textures to an appropriate 256px size to lower decoded bitmap
  memory without reducing their on-screen quality.
- Reduced expensive drone simulation updates by 25% while preserving movement and
  event-effect speed, cached fleet speed modifiers per tick, and indexed active
  debris targets for faster lookup on larger fleets.
- Smoothed planet presses and combo feedback, slowed decorative planet rotation,
  and capped simultaneous floating reward labels to reduce visual noise and frame load.
- Rebalanced globally scaled rewards with a stronger late-game curve and readable
  rounding so quests and achievements remain useful without displaying uneven values.
- Simplified the navigation artwork into cleaner cel-shaded icons with fewer
  reflections, textures, and tiny details.
- Restored the richer original application icon after visual review.
- Kept disabled drones visible instead of removing them from the game field, cleared
  stale target assignments when targets disappear or repairs finish, and added a
  bounded patrol-point fallback so drones cannot become stuck during navigation.
- Added live second-by-second reset timers to daily and weekly quest sections and
  automatic calendar-based quest rotation at local midnight and the start of each week.
- Fixed patrol avoidance trapping returned drones at the exact center of the planet;
  drones inside the drop-off zone can now fly outward toward their next patrol target.
- Restyled quest reset timers as compact cosmic clock capsules with a lightweight
  vector-drawn orbit icon and aligned monospaced countdown digits.
- Positioned floating tap rewards at the actual touch point on the planet instead
  of always spawning them at the center of the game field.
- Preserved the dismissed start screen across configuration changes so rotating the
  device no longer shows the tap-to-continue prompt again.
- Updated English and Russian event descriptions to explain the new objectives.
- Removed 86 unreferenced legacy images, reducing packaged source assets by about
  16.3 MiB without removing any resource used by the game.
- Verified Kotlin and Android resource compilation, all 78 unit tests, and Android lint.

## 1.13.0 - 2026-08-09

- Replaced the complete visual set with a cohesive atlas of 20 unique planets and
  29 progressively rarer salvage drones, including dedicated late-game worlds.
- Removed rectangular touch flashes from transparent event sprites, including the
  black hole, golden asteroid, pirate raid, and trading ship.
- Reworked the trading-ship market composition so its title leads the screen and
  the ship is centered directly below the introductory text.
- Added validated transparent edges to the new game atlases and kept existing save
  identifiers compatible with the updated resources.
- Verified debug compilation, all 86 unit tests, Android lint, release lint, R8
  minification, resource shrinking, signing, and release APK packaging.

## 1.12.0 - 2026-08-08
1
- Prepared the first store-ready release: added release signing, code and resource
  shrinking, and the permanent `com.orbitsalvagers.droneclicker` application ID.
- Rebalanced all four Command Center bosses with a smoother difficulty curve,
  clearer shield cycles, capped fighter waves, and a fairer final dragon battle.
- Fixed minion taps also damaging the boss before hitting their selected target.

- Rebuilt the Command Center around four sequential extreme boss challenges with
  persistent unlock progress, visible HP, timers, debris rewards, and prestige rewards.
- Added the Void Leviathan (100M HP), Solar Devourer (500M HP), Dreadnought
  Empress (1B HP), and Nebula War Dragon (5B HP), each with unique mechanics.
- Added regenerating shields, exposed-core damage rules, interceptor armies,
  fleet-damage reduction, dragon regeneration, minion waves, and temporary drone
  shutdowns during boss battles.
- Moved challenge battles from the Command Center dialog into the main game field,
  where bosses replace the planet and fight alongside the visible player drone fleet.
- Replaced rectangular boss illustrations with four transparent full-body boss
  renders and added entrance, hovering, breathing, tilt, and hit-reaction animations.
- Added clear victory and timeout results, floating damage values, boss ability
  indicators, and direct tap-to-attack interaction on the boss model.
- Added a custom Command Center navigation icon and localized labels below the
  Quests, Shop, Hangar, and Command Center buttons.
- Added looping background music plus dedicated planet-unlock and achievement
  reward sounds, with automatic pause and resume when the app changes state.
- Added English, Russian, and Spanish text for navigation, challenges, boss traits,
  combat instructions, and battle results.
- Added Common, Rare, and Legendary cases with increasing prices and improved
  Epic and Legendary drone odds.
- Added 24 custom case sprites: eight aligned opening frames for each case tier.
- Kept the selected case tier visible through the complete closing transition.
- Replaced the Cyber Virus instant action with a five-step 3x3 terminal puzzle,
  including success rewards and failure penalties.
- Doubled drone travel and patrol speed.
- Rebalanced Meteor Shower debris to spawn half as often, fall at half speed, and
  grant half the previous reward.
- Enforced the ten-drone fleet capacity when loading saves and awarding case drones.
- Added localized case and Cyber Virus text in English, Russian, and Spanish.

## 1.11.3 - 2026-07-24

- Centered multiline start-screen prompts, including the Russian translation.

## 1.11.2 - 2026-07-24

- Added regression tests for planet purchases and selection.
- Added tests for case-price growth, Hotel Debt repayment, precise large-number saves, and reward boundaries.
- Added coverage ensuring all 20 planets have unique localized name, description, and bonus resources.

## 1.11.1 - 2026-07-24

- Kept Black Hole debris rewards within the reachable game area.
- Fixed floating tap-income text positioning, color, rise, and fade animation.
- Made the settings screen scrollable on small screens and with large text.

## 1.11 - 2026-07-24

- Added the missing localized names, descriptions, and bonuses for planets 16–20.
- Aligned asteroid and black-hole visuals with their logical game-area coordinates.
- Preserved late-game currency accurately with backward-compatible 64-bit saves.
- Reworked Hotel Debt into a 1M loan repaid by 30% of tap income without clearing the player balance.

## 1.10.6 - 2026-07-24

- Animated the start-screen prompt with the same floating fade style used by the case-opening text.

## 1.10.5 - 2026-07-24

- Added a localized tap-to-continue prompt to the bottom of the start screen.
- Made the prompt update automatically when the game language changes.

## 1.10.4 - 2026-07-22

- Restored 11 empty PNG resources from the original sprite sheets.
- Fixed the shop crash when rendering the Debris Harvester or Signal Beacon.
- Added an automated test that validates every drawable PNG can be decoded.

## 1.10.3 - 2026-07-22

- Increased the Mystery Case price by 20% after every purchase.
- Persisted the case purchase count across game restarts.
- Migrated existing saves using the number of currently owned drones.

## 1.10.2 - 2026-07-22

- Aligned planet, drone, and debris rendering to the same game-area coordinates.
- Made returning drones visibly reach the planet before delivering debris.
- Reduced the planet avoidance radius to better match its visible size.

## 1.10.1 - 2026-07-22

- Added a localized How to Play guide to the settings screen.
- Documented taps, debris rarity, drone collection, cases, events, meteors, and planet bonuses.

## 1.10 - 2026-07-22

- Added localized bonus descriptions below every planet in the shop.
- Added a 15% chance for Sylva taps to grant double income.
- Added the missing localized description for Mars.

## 1.9.1 - 2026-07-22

- Renamed the game to Orbit Salvagers: Drone Clicker.

## 1.9 - 2026-07-21

- Renamed drawable assets with consistent category prefixes.
- Updated static, dynamic, and image-slicing references to the new resource names.

## 1.8.1 - 2026-07-21

- Resized and cropped the meteor artwork to match the debris image canvas.
- Replaced the temporary meteor symbol with the meteor image during debris showers.

## 1.8 - 2026-07-21

- Added randomized currency rewards to every debris object.
- Increased rewards up to 5K for common, 20K for rare, 50K for epic, and 1M for legendary debris.
- Applied the legendary reward range to successfully collected meteors.

## 1.7 - 2026-07-21

- Added a 20% meteor spawn chance during debris showers.
- Added a 50% chance for collected meteors to disable a drone for one minute.
- Successful meteor collection now grants the legendary debris reward.

## 1.6 - 2026-07-20

- Replaced the collapsed shop bar with a bottom-left shop image button.
- Added lock artwork to unowned planets and click upgrades until their first purchase.

## 1.5.2 - 2026-07-20

- Moved rarity names and the case opening prompt to localized resources.
- Added English, Russian, and Spanish translations for the migrated text.

## 1.5.1 - 2026-07-20

- Added a compact app name, version, and build code caption to the settings header.

## 1.5 - 2026-07-20

- Made debris shower objects collectible by drones.
- Replaced debris placeholders with the musor1 through musor6 artwork.
- Added random drone patrols that avoid flying through the planet.

## 1.4 - 2026-07-20

- Added a debris shower event with animated flying space junk.
- Randomized every event duration between 20 and 60 seconds.

## 1.3.1 - 2026-07-20

- Fixed crashes caused by rapidly pressing any store action.
- Made purchases, sales, planet unlocks, cases, and debt actions atomic.

## 1.3 - 2026-07-20

- Added a short sound effect when the player taps a planet.
- Reused and safely released the audio generator across rapid taps.

## 1.2.3 - 2026-07-17

- Aligned the currency amount and label on a shared text baseline.
- Stabilized the currency counter layout with monospaced digits.

## 1.2.2 - 2026-07-17

- Fixed a crash when rapidly purchasing click upgrades.
- Made click upgrade purchases atomic and protected cost calculations from overflow.

## 1.2.1 - 2026-07-17

- Fixed debris and drones rendering in different coordinate spaces.
- Made drones reach debris before collecting it.
- Prevented multiple drones from targeting the same debris.

## 1.2 - 2026-07-17

- Added five debris rarity tiers with increasing rewards.
- Made drone rarity determine which debris tiers it can collect.
- Removed drone-based income so rewards now come from collected debris only.
- Added rarity-colored debris visuals and drone rarity details in the fleet shop.

## 1.1.1 - 2026-07-17

- Fixed a crash caused by rapid taps on the planet.
- Limited simultaneous floating reward animations to improve stability.

## 1.1 - 2026-07-17

### Version changes

- Added an in-game settings screen.
- Added application version, language, and developer information.
- Added persistent system, English, and Russian language selection.

### Code changes

- Added Spanish resources and Spanish language selection.
- Applied English, Russian, and Spanish resources throughout the game interface.

## 1.0 - 2026-07-17

- Added animated case opening with random drone rewards.
- Added new planet, drone, and item artwork.
- Added application versioning and English project documentation.

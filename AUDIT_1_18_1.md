# Drona Salvage 1.18.1 audit

## Scope reviewed

Game screen, navigation, shop and upgrades, quests, achievements, statistics,
39-planet route, drones, cases, events, prestige, settings, persistence,
offline progress, economy, localization, animations, images, Compose theme,
shared UI components, unit tests, and instrumented test definitions.

## Resolved critical issues

- Preserved the complete 39-planet route. Valid progress on planets 26-39 is no
  longer normalized to planet 25.
- Kept late-world click, fleet, salvage, drone-discovery, and prestige gates.
- Added explicit save normalization and regression coverage for malformed and
  older planet identifiers.
- Corrected the shared cosmic button so contextual event and purchase colors are
  actually rendered instead of silently ignored.

## Resolved important issues

- Added overall completion categories and a Galactic Collection assembled from
  existing journey data.
- Added a dedicated statistics screen and background.
- Added a seven-day reward cycle without replacing daily or weekly quests.
- Added ten late-game click upgrades with ten distinct icons.
- Replaced generic settings selection and switch controls with project-specific UI.
- Ensured reduced-motion stops the main infinite background, particle, planet, and
  start-prompt animations.
- Kept encountered event types outside the rolling event-log window.

## Visual and resource work

- Unified shared spacing, typography, cards, sheets, buttons, and state colors.
- Added dedicated language, sound, motion, and reset icons.
- Replaced fixed-aspect PNG button frames with adaptive Compose controls so compact,
  wide, and translated buttons keep the same geometry without nested outlines.
- Removed neutral outer frames from major sheets and cards; outlines now communicate
  selection, availability, or an unclaimed reward instead of decorating every block.
- Replaced all nine event-type icons with a single frame-free minimal family and
  routed banners, dialogs, field meteors, and event help through the new resources.
- Removed 13 confirmed-unreferenced legacy PNG resources. They remain recoverable
  from Git history.
- Current drawable-nodpi inventory: approximately 64 MB on disk and 241 MB if every
  PNG were decoded simultaneously. Screens load only their relevant resources, but
  memory profiling on a low-RAM device is still recommended.

## Verification completed

- Kotlin debug compilation: passed.
- 132 JVM unit and regression tests: passed.
- Android lint: passed with one non-blocking dependency-update warning.
- EN, RU, and ES: 544 matching keys each; no placeholder signature mismatches.
- Economy simulations cover early, middle, late, case, upgrade, and offline curves.
- Drawable references and duplicate hashes were checked after the resource cleanup.

## Manual/device checks still required

- Run connected Compose tests when the local Gradle wrapper lock is free.
- Verify a small phone, tablet, split-screen, and large font scale.
- Profile memory and frame timing during events, a full hangar, and case opening.
- Manually traverse click -> upgrade -> drone -> planet -> event -> quest ->
  achievement -> reward -> prestige on a disposable save.
- Produce and inspect screenshots for EN, RU, and ES after the final APK is installed.

## Intentionally unchanged

- Drone models, rarity system, traits, and balance.
- Existing save keys, except additive backward-compatible progression fields.
- Core event identities and existing quest cadence.

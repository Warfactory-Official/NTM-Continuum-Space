# Changelog

Each release has a `## <version>` section; the release workflow uploads only the section matching
`mod.version` in gradle.properties.

## 1.0.2

Requires Golem Core 1.0.5 and NTM - Continuum 1.0.5.

### Added
- Generated item tags for Space items (the NTM - Continuum categories, plus `hbm:hazardous/neutron`).

### Changed
- With Particle Rain or Pretty Rain installed, the overworld's rain is left to them; other bodies keep Space's weather
  (NTM - Continuum issue 116).
- The Dyson TU converter gives heat only from its two top cells, as heat consumers now ask the machine where its heat is.

### Fixed
- The air vent and air scrubber crashed 1.21.1 Fabric and 1.20.1 with an `AbstractMethodError` (issues 4, 5; NTM -
  Continuum issue 164).
- The glitch hazard tooltip could crash the game when the creative search or JEI built tooltips off the main thread,
  for example with C2ME (issue 4).
- A boarded rocket launched at once without waiting for jump (issue 8).
- The pilot floated beside the capsule in third person during transfer (issue 8, fixed in Golem Core 1.0.5).
- Crops and Duna ores drew on black or blue squares on legacy Fabric, and tinted items were grey on 1.21.1 and 1.20.1
  (issue 8; NTM - Continuum issue 157).
- Neutron-activated items re-equipped in hand every tick (issue 6, fixed in NTM - Continuum 1.0.5).
- The ore slopper's recipe listed every bedrock ore in one row; it shows one row per body (issue 7). The patrol drone
  was listed twice in NTM - Continuum's machines tab, which crashed the NeoForge creative inventory and dropped the tab
  from JEI (issue 7).
- The Mun and Thatmo biome names were missing (NTM - Continuum issue 108).
- Space's sun, planets and skyboxes vanished under shader packs on 1.20.1 and 1.21.1 (fixed in Golem Core 1.0.5).

## 1.0.1

Requires Golem Core 1.0.4 and NTM - Continuum 1.0.4.

### Fixed
- Vacuum and toxic-atmosphere damage knocked players around in a random direction (GitHub issue 1).
- The skybox's planet crescents cost a large part of each frame; they draw in one pass, with the same pixels (GitHub
  issue 2).
- The condenser and Leviathan item icons were invisible below 26.1 (fixed in NTM - Continuum 1.0.4).

## 1.0.0

First release of **NTM: Space**, the space addon for NTM - Continuum.

Requires Golem Core 1.0.3 and NTM - Continuum 1.0.3.

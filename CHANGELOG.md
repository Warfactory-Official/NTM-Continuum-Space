# Changelog

Each release has a `## <version>` section; the release workflow uploads only the section matching
`mod.version` in gradle.properties.

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

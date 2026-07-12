# Changelog

## 1.4.0 — Arcane Architecture

### Spell language

- Expanded from four to eight drawable elements: Fire, Water, Wind, Stone, Frost, Lightning, Aether, and Void.
- Expanded manual cores from three to four ordered sigils.
- Added Beam and Glyph forms alongside Lance, Ward, Orbit, and Burst.
- Expanded clauses from Echo/Fork/Anchor to eight options: Echo, Fork, Anchor, Seek, Relay, Trigger, Bind, and Consume.
- Expanded clause capacity from two to three ordered clauses.
- Moved compiler data and glyph recognition into `SpellSystem.java`.
- Added ten continuous channels and nine derived reaction families.

### Arena and interactions

- Added one contestable ley node to each lane.
- Added node profile storage, ownership, charge decay, claiming, amplification, hostile damping, and Relay routing.
- Added persistent Glyph fields, trigger traps, waiting enchantments, beams, shards, and profile-bearing fields.
- Added runtime profile modification through allied/hostile fields, Bind, Consume, and node traversal.
- Added Steam, Thermal Shock, Conduction, Magma, Blizzard, Shatter, Null Flux, Corrosion, and Resonance reactions.
- Added Astral Court, Ember Vault, Tidal Archive, and Shattered Crown arena rules.

### Executables

- Removed the three-slot limit.
- Added an unlimited persistent executable library.
- Added scrolling/paging, new, duplicate, delete, undo, and save actions.
- Added a four-card combat quick-select ribbon that pages through the full library.
- Preserved Ink, mana surcharge, and per-executable cooldown balancing.
- Added Mnemonic Crown and Ley Key artifacts.

### UI and VFX

- Rebuilt combat composition into a five-stage flow: Sigils → Form → Clauses → Lane → Cast.
- Added an explicit cast review with profile, reaction identity, lane, mana, Ink, and Resonance.
- Added cohesive dark-metal panels, gold accents, modern sans-serif typography, arena color theming, and responsive compact layouts.
- Added procedural sigil rendering, arcane crests, rotating inscriptions, trails, radial bloom, impact rings, particle arcs, reaction text, profile pips, and ley-node visualization.
- Rebuilt the Codex and Grimoire screens around the expanded system.

### AI

- AI now composes up to four elements and three clauses according to difficulty.
- Added visible progressive telegraphing for the expanded syntax.
- Added arena/node-aware lane selection and basic counter-profile selection.

### Build

- Bumped Android app version to 1.4.0 / versionCode 5.
- Updated README, game design, spell-system reference, and changelog.

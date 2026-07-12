# Changelog

## 2.0.0 — The Living Weave

### Spell language

- Expanded from eight to ten drawable elements: added **Radiance** (arch glyph) and **Verdance** (sprout glyph).
- Expanded the compiler from ten to twelve continuous channels (radiance, growth).
- Added four derived reactions: **Wildfire**, **Overgrowth**, **Sanctify**, and **Eclipse** (thirteen reaction families total).
- Expanded from six to nine forms: added **Surge** (shoving wavefront), **Rift** (lane-rewriting portal), and **Aura** (global enchantment).
- Expanded from eight to twelve clauses: added **Swift** (tempo mirror of Anchor), **Siphon** (drain what it crosses), **Hex** (curse a hostile object), and **Dispel** (erase a hostile spell).

### Spell-to-spell interaction

- Auras blend a share of their profile into every allied cast while standing; one per duelist, replaceable, attackable, and dispellable.
- Hex curses the strongest hostile persistent object (or waits for the next arrival), sapping damage, durability, and lifetime.
- Dispel destroys hostile spell bodies outright on contact, disarms traps without triggering them, and erases hostile fields and enchantments in passing.
- Siphon inverts field/node friction: hostile fields and node charge are drained into the spell, and construct hits leech mana.
- Consume can now sacrifice rifts and auras; sanctify reactions cleanse entropy and lift hexes from the survivor.

### Arena dynamism

- Added per-duel **lane traits** (three rolled from six): Ley Current, Bedrock, Wellspring, Wild Magic, Ashen Ground, and Thin Veil — shown in the lane picker and etched on the field.
- Added periodic **arena events** with banners: Ley Surge, Vent Eruption, Tide Surge, Fracture, Bloom, and Light Shaft.
- Added neutral environment-owned hazard fields that threaten both duelists.
- Added two arenas: **Verdant Reach** and **Radiant Basilica**.

### Artifacts and AI

- Added **Hexwright Ring** (Hex/Dispel/Siphon cheaper and stronger) and **Verdant Chalice** (growth amplified, overgrowth mends the caster).
- The rival composes with the full expanded grammar, avoids redundant auras, and counters Radiance/Verdance appropriately.

### UI and VFX overhaul

- Engraved panels: brushed-metal gradients, gradient-gold borders, filigree corner ticks, selection glows, and accent spines.
- Rotating ley-circle background watermark, vignette lighting, and drifting motes.
- Comet projectile trails, layered flickering beams with end flares, node charge-arc gauges, portal and aura renderings, hex curse geometry, and empowered orbit sparks.
- Expanding shockwave rings and screen shake on bursts, thermal shocks, eclipses, and caster hits.
- Composer rebuilt around a golden diamond step-rail with a live sentence preview and numbered stage-advance buttons; the Cast button breathes.
- HUD ink displayed as gold diamond pips; the resonance bar glows when Ascendant is ready.
- Codex expanded to five pages including a spell-interaction primer; grids updated for ten elements, nine forms, twelve clauses, and six arenas.

### Build

- Bumped Android app version to 2.0.0 / versionCode 6.
- v1.4 saved libraries and title-screen selections load unchanged (enum constants appended).
- Updated README, game design, spell-system reference, v2.0 plan, and changelog.

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

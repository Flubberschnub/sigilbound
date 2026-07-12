# Sigilbound: The Living Weave

Sigilbound is a native Android prototype for a real-time three-lane spellcasting duel. Players hand-write elemental sigils, choose a form rune, attach ordered behavior clauses, commit the sentence to a lane, and cast it into an arena that fights back — contestable ley nodes, rolled lane traits, and periodic arena events make the board itself part of every spell.

## Version 2.0 highlights

- Ten drawable elements: **Fire, Water, Wind, Stone, Frost, Lightning, Aether, Void, Radiance, and Verdance**.
- Four ordered element slots feeding a twelve-channel continuous compiler.
- Nine forms: **Lance, Ward, Orbit, Burst, Beam, Glyph, Surge, Rift, and Aura** — including a shoving wavefront, a lane-rewriting portal, and a Magic-the-Gathering-style global enchantment.
- Twelve clauses: **Echo, Fork, Anchor, Seek, Relay, Trigger, Bind, Consume, Swift, Siphon, Hex, and Dispel**. A sentence holds three ordered clauses.
- Deep spell-to-spell interaction: auras empower every allied cast, hexes curse enemy permanents, dispels erase spell bodies and traps outright, siphons drain hostile fields and nodes, and consume/bind/trigger complete the stack.
- Thirteen runtime reactions, now including **Wildfire, Overgrowth, Sanctify, and Eclipse**.
- Three contestable ley nodes — one per lane — plus **per-duel lane traits** (Ley Current, Bedrock, Wellspring, Wild Magic, Ashen Ground, Thin Veil) that make lane choice a positional decision.
- **Arena events**: vents erupt, tides surge, lanes fracture, blooms mend, light shafts consecrate, and ley nodes flare. Environmental hazards belong to no one and can be exploited by both duelists.
- Six arena rulesets: Astral Court, Ember Vault, Tidal Archive, Shattered Crown, **Verdant Reach**, and **Radiant Basilica**.
- Seven artifacts, including the **Hexwright Ring** (curse specialist) and **Verdant Chalice** (growth specialist).
- An unlimited persistent executable library with scrolling, duplication, deletion, editing, and a four-card combat quick-select ribbon.
- A five-stage mobile composer with a golden diamond step-rail and live sentence preview: **Sigils → Form → Clauses → Lane → Cast**.
- Overhauled presentation: engraved gold filigree panels, a rotating ley-circle watermark, vignette lighting, comet projectile trails, layered flickering beams, expanding shockwaves, screen shake, node charge-arc gauges, and pop-in reaction text.

## Spell language

```text
[1–4 ordered elemental sigils]
→ [one form rune]
→ [0–3 ordered clauses]
→ [one lane]
→ CAST
```

The compiler does not select a named recipe from a lookup table. Each sigil adds weighted values to twelve channels:

heat · moisture · impulse · mass · cohesion · volatility · cold · charge · aether · entropy · radiance · growth

Forms interpret those channels differently. Runtime entities then continue to gain, lose, merge, relay, bind, hex, siphon, dispel, or consume channel data through fields, nodes, clauses, auras, rifts, and collisions. This produces reactions such as steam veils, thermal shock, conduction, molten fields, blizzards, shatter, null flux, corrosion, wildfire, overgrowth, sanctify, and eclipse without requiring a bespoke spell entry for every combination.

See [`docs/SPELL_SYSTEM.md`](docs/SPELL_SYSTEM.md) for the compiler and interaction rules, [`GAME_DESIGN.md`](GAME_DESIGN.md) for the full combat design, and [`docs/V2_0_PLAN.md`](docs/V2_0_PLAN.md) for the v2.0 design plan.

## Executable library

The Grimoire has no fixed slot count. Any valid sentence can be saved, duplicated, edited, or deleted. During combat, four programs are shown at a time and the player can page through the complete library.

Executables trade flexibility for speed:

- one Ink per deployment;
- a mana surcharge;
- an individual cooldown;
- manual writing is still required to rebuild Ink efficiently.

The Mnemonic Crown reduces the mana surcharge but lowers manual Ink recovery, while other artifacts encourage different compiler, arena, and interaction strategies.

## Build

Requirements:

- JDK 17
- Android SDK platform 36
- Gradle 9.4.1

```bash
./build.sh
```

The debug APK is copied to `dist/Sigilbound-Arcane-Duel-debug.apk`.

## Automation

- `.github/workflows/android-ci.yml` runs lint and builds a debug APK on pushes and pull requests, then uploads the APK and lint reports.
- `.github/workflows/release.yml` creates an installable debug-signed GitHub Release for tags matching `v*`.

## Architecture

- `SpellSystem.java` owns spell syntax, profile compilation, serialization, costs, and glyph recognition.
- `ArcaneDuelView.java` owns the prototype simulation, AI, arena events, lane traits, input flow, persistence, and procedural rendering.
- `MainActivity.java` hosts the full-screen custom view.

The project deliberately has no third-party runtime: the entire presentation is procedural Canvas drawing, which keeps the simulation deterministic and the APK dependency-free. A production multiplayer version should split deterministic combat simulation, rendering, persistence, matchmaking, and network transport into separate modules.

## Current scope

- AI duels and practice mode are implemented.
- Network multiplayer is not yet implemented.
- The application is portrait-only.
- Visuals are generated procedurally with Android Canvas primitives.

# Sigilbound: Arcane Architecture

Sigilbound is a native Android prototype for a real-time three-lane spellcasting duel. Players hand-write elemental sigils, choose a form rune, attach ordered behavior clauses, commit the sentence to a lane, and cast it into an arena whose ley geometry can be reshaped by both duelists.

## Version 1.4 highlights

- Eight drawable elements: **Fire, Water, Wind, Stone, Frost, Lightning, Aether, and Void**.
- Four ordered element slots. Later sigils remain meaningful but contribute less strongly, and the Prism Lens changes that weighting.
- Six forms: **Lance, Ward, Orbit, Burst, Beam, and Glyph**.
- Eight clauses: **Echo, Fork, Anchor, Seek, Relay, Trigger, Bind, and Consume**. A sentence can hold three clauses in order.
- Three contestable ley nodes—one per lane—which store elemental profiles and modify spells that pass through them.
- Persistent fields, traps, enchantments, constructs, beams, projectiles, and node relays can modify one another after deployment.
- Four arena rulesets with distinct ambient channels and tactical biases.
- An unlimited persistent executable library with scrolling, duplication, deletion, editing, and a four-card combat quick-select ribbon.
- A five-stage mobile composer: **Sigils → Form → Clauses → Lane → Cast**.
- Rebuilt modern-fantasy presentation using dark metal panels, restrained gold trim, animated arcane geometry, colored spell trails, bloom-like Canvas effects, impact particles, field inscriptions, and readable sans-serif typography.

## Spell language

```text
[1–4 ordered elemental sigils]
→ [one form rune]
→ [0–3 ordered clauses]
→ [one lane]
→ CAST
```

The compiler does not select a named recipe from a lookup table. Each sigil adds weighted values to ten channels:

- heat
- moisture
- impulse
- mass
- cohesion
- volatility
- cold
- charge
- aether
- entropy

Forms interpret those channels differently. Runtime entities then continue to gain, lose, merge, relay, bind, or consume channel data through fields, nodes, clauses, and collisions. This produces reactions such as steam veils, thermal shock, conduction, molten fields, blizzards, shatter, null flux, corrosion, and resonance without requiring a bespoke spell entry for every combination.

See [`docs/SPELL_SYSTEM.md`](docs/SPELL_SYSTEM.md) for the compiler and interaction rules and [`GAME_DESIGN.md`](GAME_DESIGN.md) for the full combat design.

## Executable library

The Grimoire has no fixed slot count. Any valid sentence can be saved, duplicated, edited, or deleted. During combat, four programs are shown at a time and the player can page through the complete library.

Executables trade flexibility for speed:

- one Ink per deployment;
- a mana surcharge;
- an individual cooldown;
- manual writing is still required to rebuild Ink efficiently.

The Mnemonic Crown reduces the mana surcharge but lowers manual Ink recovery, while other artifacts encourage different compiler and arena strategies.

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
- `ArcaneDuelView.java` owns the prototype simulation, AI, input flow, persistence, and procedural rendering.
- `MainActivity.java` hosts the full-screen custom view.

The project deliberately has no third-party runtime. A production multiplayer version should split deterministic combat simulation, rendering, persistence, matchmaking, and network transport into separate modules.

## Current scope

- AI duels and practice mode are implemented.
- Network multiplayer is not yet implemented.
- The application is portrait-only.
- Visuals are generated procedurally with Android Canvas primitives.

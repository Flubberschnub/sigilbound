# Sigilbound: Arcane Duel

A small native Android prototype for a real-time three-lane spellcasting duel. Players draw elemental glyphs, compile them into spell sentences, attach generic behavior clauses, and deploy the result into a live lane.

## What changed in v1.3

- The rival visibly writes each spell before launch: target lane, progressive element glyphs, form, clauses, countdown, and channel progress.
- Projectile travel speed is controlled by a match rule: **Ritual**, **Duel**, or **Blitz**. Ritual is the default and is substantially slower.
- Manual spell cores accept up to three ordered elements.
- Generic clauses—**Echo**, **Fork**, and **Anchor**—operate on every element sequence and every form.
- Three editable **executables** can be prepared in the Grimoire and deployed quickly during combat.
- Executables cost 25% extra mana, consume Ink, and have cooldowns. Accurate manual glyphs restore Ink.
- Manual formulas no longer expire.
- The interface uses the plain Android sans-serif family, medium rather than synthetic bold weight, and larger minimum mobile text sizes.

## Spell language

```
[core element] [modifier element?] [modifier element?]
+ [clause?] [clause?]
+ [lane]
+ [form]
```

Element order matters because later glyphs contribute less weight. The compiler derives continuous channels instead of selecting a fixed pair recipe:

- Fire: heat, volatility, some impulse
- Water: moisture, cohesion, heat damping
- Wind: impulse, volatility
- Stone: mass, cohesion, impulse damping

Thresholds create vapor, molten residue, chilling drag, and fracture. Those properties then affect Lance, Ward, Orbit, and Burst through shared rules. See [`docs/SPELL_SYSTEM.md`](docs/SPELL_SYSTEM.md).

## Build

Requirements:

- JDK 17
- Android SDK platform 36 and Build Tools 36.0.0
- Gradle 9.4.1

```bash
./build.sh
```

The debug APK is copied to `dist/Sigilbound-Arcane-Duel-debug.apk`.

## Automation

- `.github/workflows/android-ci.yml` runs Android lint and builds an APK on every push and pull request, then uploads the APK and lint report.
- `.github/workflows/release.yml` creates an installable debug-signed GitHub Release for tags matching `v*`.

## Architecture

The prototype intentionally has no engine or third-party runtime. `ArcaneDuelView` owns rendering, touch recognition, simulation, AI, and tutorial state using Android Canvas primitives. That keeps the APK tiny, though a production version should split the compiler, combat simulation, rendering, and networking into testable modules.

## Current scope

- AI duels are implemented.
- Network multiplayer is not implemented in this standalone prototype.
- The APK is portrait-only and uses procedural artwork.

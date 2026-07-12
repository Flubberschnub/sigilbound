# Sigilbound v2.0 Implementation Notes

## Architecture

The v2.0 update keeps the two-file split introduced in v1.4:

- `SpellSystem.java` owns elemental channels (now twelve), spell forms (nine), ordered clauses (twelve), artifacts (seven), arena definitions (six), executable serialization, costs, and gesture templates (ten).
- `ArcaneDuelView.java` owns the mobile interaction flow, arena simulation (including arena events and lane traits), AI composition, runtime spell interactions, procedural rendering, and persistence.

The renderer remains a zero-dependency custom `View` drawing with Canvas primitives. A stack migration (Compose/libGDX) was considered and rejected: the presentation is fully procedural and frame-driven already, and the overhaul goal was achievable by rebuilding what is drawn rather than how.

## New runtime systems

- **Environment owner:** hazard fields spawned by arena events use owner index `2` (`OWNER_ENVIRONMENT`), which makes every existing "hostile if owner differs" comparison treat them as dangerous to both duelists without new code paths.
- **Arena events** fire on an 11–16 s timer (`fireArenaEvent`) and are announced via the banner system. Tide Surge and Light Shaft are global/lane timers consulted by the projectile update; the others spawn entities.
- **Lane traits** are rolled in `resetCombat` (three distinct from six) and consulted at deploy time (Bedrock, Ley Current), in the reaction resolver (Wild Magic), in field creation (Ashen Ground, Wild Magic), in node attunement (Thin Veil), and in the per-frame node loop (Wellspring).
- **Aura** entities are found via `findAura(owner)`; `deploySpell` blends 35% of a standing aura into every allied non-aura cast. Casting a new aura kills the previous one.
- **Rift** redirection lives in `applyRiftInfluence`, called from the projectile field-scan loop. Redirect count is stored in the entity's `timer` field (three charges).
- **Surge** entities use their own update path (`updateSurge`): push hostile projectiles backward, damp hostile fields, wash over constructs as a single hit.
- **Hex** resolves pre-deployment (`applyHex` / waiting hex enchantments flagged `hexWaiting`), mirroring Bind's structure.
- **Dispel** is checked in three places: spell-to-spell contact (before reaction analysis), trap proximity (disarm without release), and hostile field crossings (erase). The clause is consumed on a successful spell dispel.
- **Siphon** branches in `applyFieldInfluence` and `interactWithNode`, and leeches mana in `applyHitToEntity`.

## Presentation systems added

- `Shockwave` list: expanding stroked rings with fade, used for impacts, detonations, events, and dispels.
- Screen shake: `shakeTimer`/`shakeMagnitude` translate the arena canvas layer only (the composer stays still).
- `uiClock` runs on every frame regardless of screen so title/menu animation and button pulses never freeze.
- Comet trails render the entity trail as tapering connected segments instead of dot circles.
- `drawMetalPanel` rebuilt: three-stop brushed gradient, gradient-gold engraved border, filigree corner ticks on larger panels, radial glow on selection.
- Ley nodes gained a charge-arc gauge and counter-rotating inner geometry; lanes carry etched trait labels.
- The composer step rail is a golden diamond rail with completion fill and an active-station glow, plus a right-aligned live sentence preview.

## Compatibility

- All new enum constants are appended, so v1.4 serialized programs (`name|ELEMENTS|FORM|CLAUSES`) and stored ordinal selections (artifact/difficulty/tempo/arena) load unchanged.
- The SharedPreferences namespace remains `sigilbound_v14`.
- `decode` still caps at four elements and three clauses.

## Validation

Before publication, both Java sources were compiled against the Android platform-36 `android.jar` using JDK 21 (`javac -cp android.jar`). GitHub Actions performs the authoritative Android lint and debug APK assembly using the repository's pinned Gradle and Android toolchain.

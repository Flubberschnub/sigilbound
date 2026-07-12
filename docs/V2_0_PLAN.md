# Sigilbound v2.0 — The Living Weave: Design Plan

This document is the implementation plan for the v2.0 overhaul. It has two goals:

1. **Deepen the spell system** — more elements, forms, and clauses; richer spell-to-spell
   and spell-to-arena interaction in the spirit of Outward's environmental synergies,
   Magicka 2's combinable, friendly-fire-adjacent chaos, Tyranny's expression-based spell
   crafting, and Magic: the Gathering's "everything can modify everything" stack.
2. **Overhaul the presentation** — replace the prototype programmer art with a cohesive
   modern-fantasy visual language: near-black lacquer, brushed charcoal metal, engraved
   gold filigree, ivory type, and arcane procedural VFX with real impact.

## Design references

- **Outward:** the environment is a spell component. Rain, heat, and standing effects
  change what a cast does. → Sigilbound arenas gain ambient channels, periodic *arena
  events*, and per-duel *lane traits* so the board itself is part of every sentence.
- **Magicka 2:** free element stacking with emergent (and dangerous) combinations. →
  The continuous channel compiler already avoids recipe tables; v2.0 widens it to twelve
  channels and thirteen runtime reactions so more sigil orders produce distinct behavior.
- **Tyranny:** spells are sentences — core + expression + accent, gated by grammar. →
  The Sigils → Form → Clauses grammar stays, but grows two new elements, three new forms,
  and four new clauses, including ones that talk *about other spells*.
- **Magic: the Gathering:** spells and permanents modify each other after they resolve. →
  v2.0 adds a permanent type (Aura), targeted enchantment/curse (Bind/Hex), removal
  (Dispel), theft (Siphon), and sacrifice (Consume) so the mid-board is a stack of
  interacting objects, not a bullet corridor.

## 1. Spell language expansion

### 1.1 Elements: 8 → 10

| Element | Glyph | Channels | Identity |
|---|---|---|---|
| Radiance | arch (∩) | radiance, heat, cohesion, −entropy | revelation, searing light, cleansing |
| Verdance | sprout (✓) | growth, moisture, cohesion | life, regrowth, entanglement |

The channel vector grows from ten to twelve: `heat, moisture, impulse, mass, cohesion,
volatility, cold, charge, aether, entropy, radiance, growth`.

New derived reactions (recomputed on every merge/scale, same as v1.4):

- **Wildfire** = heat × growth — spreading burn fields that creep along the lane.
- **Overgrowth** = growth × moisture — entangling regrowth; heals allied constructs,
  slows hostile bodies.
- **Sanctify** = radiance × (cohesion + aether) — cleansing shield; strips entropy and
  hexes from allies.
- **Eclipse** = radiance × entropy — blinding erasure; heavy single-hit damage bonus.

### 1.2 Forms: 6 → 9

- **Surge** (15 mana): a wide, slow wavefront. It does not trade with hostile
  projectiles — it *shoves them backward* and damps hostile fields it crosses. A pure
  positioning/tempo tool (the Magicka "push" verb).
- **Rift** (20 mana): a portal inscribed at the lane's forward third. Hostile
  projectiles that enter are redirected into another lane (whichever threatens its owner
  least); allied projectiles that pass gain speed and inherit part of the rift profile.
  Carries three redirect charges, then collapses. Rewrites board geometry.
- **Aura** (22 mana): a *global enchantment* — the MtG permanent. While the aura stands
  (one per caster; recasting replaces it), every allied cast blends a share of the aura's
  profile into the new spell. It is a fragile construct sitting beside the caster: it can
  be beamed, hexed, consumed, or dispelled.

### 1.3 Clauses: 8 → 12

- **Swift** (4): faster and leaner — trades size and a little damage for speed. The
  tempo mirror of Anchor.
- **Siphon** (6): inverts field/node friction. Hostile fields and nodes the spell
  crosses are *drained into it* (profile, charge, damage) instead of weakening it.
  Construct hits leech HP into caster mana.
- **Hex** (6): the hostile mirror of Bind. Curses the strongest hostile persistent
  object in the lane — merging inverted entropy pressure, cutting its damage and HP, and
  eroding its remaining lifetime. With no target, a waiting hex lingers and curses the
  next hostile arrival.
- **Dispel** (5): the removal instant. The spell body becomes a counterspell: on
  spell-to-spell contact the hostile body is destroyed outright regardless of power (the
  dispel charge is consumed), and hostile enchantments, auras, and fields it passes are
  erased.

### 1.4 Artifacts: 5 → 7

- **Hexwright Ring:** Hex, Dispel, and Siphon are stronger and cheaper; hexes last longer.
- **Verdant Chalice:** growth channels amplified; overgrowth healing also mends the
  caster.

## 2. Arena dynamism

### 2.1 New arenas: 4 → 6 (appended, preserving saved selection indices)

- **Verdant Reach:** ambient growth + moisture. Overgrowth strategies and construct
  sustain thrive.
- **Radiant Basilica:** ambient radiance + cohesion. Beams, sanctify shields, and
  eclipse burst casts are favored.

### 2.2 Arena events (every ~11–16 s, announced with a banner)

| Arena | Event | Effect |
|---|---|---|
| Astral Court | Ley Surge | a random node gains charge for its current owner |
| Ember Vault | Vent Eruption | neutral molten hazard field erupts in a random lane |
| Tidal Archive | Tide Surge | all projectiles slowed and soaked for a few seconds |
| Shattered Crown | Fracture | shatter hazard field tears open in a random lane |
| Verdant Reach | Bloom | regrowth field heals **all** constructs in a lane |
| Radiant Basilica | Light Shaft | a lane is illuminated; spells inside gain damage and radiance |

Hazard fields are *neutral* (owner = environment): they treat both duelists as hostile
and can be exploited by whoever positions around them best.

### 2.3 Lane traits (rolled per duel, shown in the lane picker and on the field)

Three distinct traits are drawn each duel from: **Ley Current** (faster projectiles),
**Bedrock** (sturdier constructs), **Wellspring** (owning the node grants mana regen),
**Wild Magic** (stronger reactions, longer fields), **Ashen Ground** (fields persist
longer), **Thin Veil** (node charges faster). Lane choice becomes a real positional
decision even before considering enemy pressure.

## 3. UI/UX overhaul

**Tech stack decision:** stay on the zero-dependency custom `View` + Canvas renderer.
The entire presentation is procedural, deterministic, and already draws at animation
frame rate; a Compose/libGDX migration would rewrite 3k lines for no gameplay gain. The
overhaul instead rebuilds *what* is drawn.

### Visual language

- Near-black lacquer background with radial vignette, drifting gold motes, and a huge
  slow-rotating ley-circle watermark behind the arena.
- **Ornate panels:** brushed-metal vertical gradient, engraved double border in gradient
  gold, filigree corner ticks, accent spine on selection, soft glow on active elements.
- Elemental neon reserved for spell bodies and glyphs so gold/ivory UI stays legible.

### VFX

- **Shockwaves:** expanding stroked rings on impacts, detonations, node claims, casts.
- **Screen shake** on bursts, thermal shock, and caster hits.
- **Comet projectiles:** tapered gradient trails, glow cores, orbit sparks when
  empowered.
- **Layered beams:** halo + column + ivory core with animated flicker and end flare.
- **Ley nodes as vortices:** counter-rotating rune rings with a charge arc gauge.
- Floating reaction text with pop-in scale; reaction color coding.

### UX flow

The composer keeps the five-stage sentence — **Sigils → Form → Clauses → Lane → Cast** —
with a clearer diamond step-rail (completed stages fill gold), a live sentence preview
under the rail at every stage, bigger stage-advance buttons that name the next stage,
and a pulsing CAST button showing the exact mana cost. Grids grow to 3×3 forms and 3×4
clauses. The codex gains pages for the new content and one for spell-interaction rules.

## 4. Compatibility & migration

- New enum constants are appended, so v1.4 saved programs (`name|ELEMENTS|FORM|CLAUSES`
  strings) and title-screen selections (stored ordinals) load unchanged.
- SharedPreferences namespace stays `sigilbound_v14`.
- `versionName` bumps to 2.0.0, `versionCode` to 6.

## 5. Implementation order

1. `SpellSystem.java`: channels, elements, forms, clauses, artifacts, arenas, costs,
   glyph templates, compile ambience.
2. `ArcaneDuelView.java` runtime: new entity kinds and clause behavior, arena events,
   lane traits, AI awareness of the new grammar.
3. `ArcaneDuelView.java` presentation: panel/VFX/HUD/composer overhaul, expanded grids
   and codex.
4. Documentation: README, GAME_DESIGN, SPELL_SYSTEM, CHANGELOG, implementation notes.

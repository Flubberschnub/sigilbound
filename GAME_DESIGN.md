# Sigilbound v1.3 — Readable Syntax

## Goals

1. Make every hostile cast readable before it resolves.
2. Slow battlefield travel enough that each lane becomes a visible contest rather than a reaction test against sudden damage.
3. Replace fixed two-element recipes with a compositional spell compiler.
4. Preserve the fast tactile identity of drawing sigils while allowing deeper authored playstyles through prepared spell executables.

## Duel Rhythm

A duel alternates between **reading**, **writing**, and **resolving**.

- The rival visibly scribes a spell for several seconds. Its element glyphs, target lane, form, and clauses are revealed in stages.
- The player may answer manually with a fresh formula or deploy a prepared executable.
- Spells then move through one of three lanes at the selected match tempo. Multiple casts can coexist in each lane, producing small simultaneous duels.

The default rules are intentionally slow and legible. Faster rules remain available for experienced players.

## Spell Grammar

A manual spell has this syntax:

`CORE ELEMENT → up to 2 MODIFIER ELEMENTS → FORM RUNE → up to 2 CLAUSES → LANE`

### Ordered elements

The first element is the core identity and determines the strongest visual and counter affinity. The second and third elements contribute weighted physical channels rather than selecting a handcrafted recipe.

- **Fire:** heat, volatility, damage, burning.
- **Water:** moisture, breadth, damping, slowing.
- **Wind:** impulse, speed, repetition pressure.
- **Stone:** mass, cohesion, durability, impact.

The compiler derives heat, moisture, impulse, mass, cohesion, and volatility from the complete ordered sequence. Threshold effects emerge from those channels:

- heat + moisture creates vapor and obscuring steam;
- heat + mass leaves molten residue;
- moisture + impulse creates chilling drag;
- mass + impulse creates fracture and shrapnel;
- excess cohesion makes constructs tougher;
- excess volatility makes effects stronger but less stable.

A third element can satisfy several thresholds simultaneously, so the same form can produce very different behavior without a table of named pair recipes.

### Form runes

- **Lance:** moving projectile and primary interception tool.
- **Ward:** persistent blocking construct.
- **Orbit:** familiar that periodically emits compiled bolts.
- **Burst:** delayed field event placed deep in a lane.

### Clause runes

Any spell can contain up to two clauses.

- **Echo:** repeats the compiled spell after a delay at reduced strength.
- **Fork:** deploys reduced branches into adjacent lanes.
- **Anchor:** slows movement while increasing persistence, mass, construct durability, and residue strength.

Because clauses operate on the compiled spell rather than on bespoke named recipes, combinations such as Echo + Fork or Fork + Anchor work on every element sequence and every form.

## Enemy Telegraphing

The AI no longer creates spells instantly. It creates an `EnemyCastIntent` and reveals it over a channel period.

1. Target lane appears immediately.
2. Element glyphs are drawn one by one.
3. Form rune is revealed.
4. Clauses are revealed.
5. The spell launches.

Apprentice channels are long and contain idle gaps. Adept channels are moderate. Archmage channels are faster but still visible.

## Tempo Rules

- **Ritual:** 42% projectile travel speed. Default.
- **Duel:** 62% projectile travel speed.
- **Blitz:** 84% projectile travel speed.

Tempo affects projectile movement and enemy channel pacing, but not touch recognition.

## Executable Decks

Players own a three-slot grimoire edited outside combat. Each slot stores a complete spell program: up to three elements, one form, and up to two clauses.

During combat, an executable can be armed with one tap and deployed by selecting a lane.

### Convenience cost

- Executables cost 25% more mana.
- Executables consume one **Ink**.
- The player begins with three Ink.
- Accurate manual glyphs restore Ink, rewarding alternation between prepared and hand-written spells.
- Each executable slot has a short cooldown.

This creates decks with recognizable loops without allowing prepared spells to replace manual skill.

## Readability Rules

- Rival target lane is tinted from the start of the cast.
- Rival syntax is shown as progressively drawn glyphs and readable form/clause chips above the battlefield.
- Projectiles carry element pips and longer trails.
- Default projectile speed is substantially lower.
- Large impact text distinguishes COUNTER, VAPOR, FRACTURE, BLOCK, and RESIDUE.
- UI copy uses a plain system sans-serif face, medium emphasis rather than synthetic bold, and no labels below roughly 10.5sp.
- Manual and executable modes are visually separated.

## AI Profiles

### Apprentice
- Long initial grace period.
- 5–7 second channels.
- Long recovery between casts.
- Mostly one-element spells, rare clauses, imperfect counters.

### Adept
- 3.5–5 second channels.
- Uses two elements and occasional clauses.
- Reads lane pressure with moderate accuracy.

### Archmage
- 2.5–4 second channels.
- Uses three elements and clause pairs.
- Better counter selection and lane pressure.

## Scope of v1.3

This version implements AI telegraphing, selectable tempo, three-element compilation, generic channel-derived spell stats, three compositional clauses, a three-slot executable grimoire, Ink economy, and GitHub build automation. Network transport remains outside this standalone prototype.

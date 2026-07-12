# Spell Compiler and Duel Economy

## Design target

The spell system should feel authored by the player without forcing them to write a long program under combat pressure. A cast therefore has two layers:

1. **Core composition**: one to three ordered elements.
2. **Behavior composition**: one form and up to two generic clauses.

## Continuous channels

Each element contributes weighted channels. The first glyph has weight 1.0, the second 0.72, and the third 0.52.

The compiler calculates heat, moisture, impulse, mass, cohesion, and volatility. Derived thresholds are calculated from interactions between channels:

- vapor = heat × moisture
- molten residue = heat × mass
- chilling drag = moisture × impulse minus heat
- fracture = mass × impulse

This means a three-element core can satisfy several thresholds at once. There is no lookup table that says “Fire + Wind equals a bespoke spell.” The same channel profile is consumed by every form.

## Forms

- **Lance** turns channels into projectile speed, damage, radius, collision power, and residue.
- **Ward** turns channels into durability, lifetime, elemental resistance, and stability.
- **Orbit** turns channels into familiar durability, firing cadence, bolt profile, and lifetime.
- **Burst** turns channels into delay, radius, detonation damage, field residue, and status effects.

Volatility increases offensive output and shortens delays, but reduces persistent construct stability.

## Clauses

Clauses transform the compiled result rather than selecting bespoke variants:

- **Echo** schedules a reduced copy after a delay.
- **Fork** deploys reduced copies into adjacent lanes.
- **Anchor** slows movement and increases persistence, mass, construct durability, and residue.

Because the transformations are generic, `Fork + Echo` works on a Water Ward, a Stone Burst, or a three-element Orbit without a separate implementation for each combination.

## Executable deck

The player edits three complete spell programs outside combat. In a duel, tapping an executable arms it and tapping a lane deploys it.

Convenience is balanced by:

- 25% extra mana cost
- one Ink per execution
- per-slot cooldown
- Ink regeneration from accurate manual glyphs

The intended loop is to open with a prepared play, manually write efficient counters while rebuilding Ink, and then execute a deck payoff when multiple lanes are aligned.

## Readability

An enemy cast is represented as an explicit channel:

1. target lane is highlighted immediately;
2. element glyphs are drawn progressively;
3. form is revealed;
4. clauses are revealed;
5. the completed spell launches.

Ritual tempo intentionally allows several seconds of projectile travel so the player can answer a lane rather than taking unavoidable damage after the telegraph.

# Sigilbound v1.4 Implementation Notes

## Architecture

The v1.4 update separates spell-language concerns from duel presentation and simulation:

- `SpellSystem.java` owns elemental channels, spell forms, ordered clauses, executable serialization, costs, arena rules, and gesture templates.
- `ArcaneDuelView.java` owns the mobile interaction flow, arena simulation, AI composition, runtime spell interactions, procedural rendering, and persistence.

A spell program contains up to four ordered elemental sigils, one form rune, and up to three ordered clauses. Compilation produces continuous physical and arcane channels rather than selecting a named recipe. Runtime systems then compare those channels when projectiles, constructs, fields, nodes, and enchantments interact.

## Runtime interaction model

The arena exposes three lanes and three contestable ley nodes. Effects may persist as fields, glyphs, traps, wards, orbitals, beams, or shards. Clauses can repeat, fork, anchor, seek, relay, trigger, bind, or consume other effects.

Generic reaction rules cover steam, thermal shock, conduction, magma, blizzard, shatter, null flux, corrosion, and resonance. Arena variants alter node behavior and environmental modifiers without replacing the spell grammar.

## Mobile UX

Manual casting follows one visible sequence:

1. Sigils
2. Form
3. Clauses
4. Lane
5. Cast

The grimoire stores an unbounded executable library. During combat, a paged four-card ribbon exposes quick casts while preserving lane commitment, mana cost, Ink cost, and cooldown balance.

## Validation

Before publication, both Java sources were compiled against an Android API compatibility JAR using Java 17. GitHub Actions performs the authoritative Android lint and debug APK assembly using the repository's pinned Gradle and Android toolchain.

# Sigilbound v1.4 — Arcane Architecture

## Design objective

Sigilbound should feel like a real-time card battler whose cards are partially written during play. The player does not merely choose an attack; they author a small executable sentence, deploy it into one of three simultaneous lane contests, and then exploit or alter the persistent state left behind by previous spells.

The redesign has four pillars:

1. **Readable authorship:** the input flow always reads Sigils → Form → Clauses → Lane → Cast.
2. **Compositional depth:** elemental properties and generic clauses combine through shared rules rather than a list of handcrafted pair recipes.
3. **Arena memory:** nodes, fields, traps, constructs, and enchantments persist long enough for one spell to set up another.
4. **Prepared playstyles:** an unlimited executable library lets players construct repeatable loops, while Ink, mana surcharge, and cooldowns keep manual writing relevant.

## Duel topology

The arena still has three lanes, but every lane now contains a ley node at mid-field. A lane therefore contains several overlapping contests:

- spell versus spell interception;
- construct versus pressure;
- control of the ley node;
- persistent field ownership;
- trap and trigger positioning;
- future relay routes.

A projectile that passes through an allied node absorbs part of the node profile and gains force. A hostile node weakens it and can produce a reaction. Glyph forms claim and tune nodes. Relay clauses route a spell through an allied charged node into a more useful lane. This turns the three lanes from simple projectile tracks into a small programmable board.

## Spell grammar

`ELEMENT × 1–4 → FORM × 1 → CLAUSE × 0–3 → LANE → CAST`

### Ordered elemental core

Each position has a descending weight. The first sigil establishes the strongest identity, while later sigils modify physical and arcane channels. The Prism Lens preserves more of the later positions.

- **Fire:** heat, volatility, ignition pressure.
- **Water:** moisture, cohesion, damping.
- **Wind:** impulse, spread, instability.
- **Stone:** mass, cohesion, structural durability.
- **Frost:** cold, brittleness, thermal opposition.
- **Lightning:** charge, impulse, volatile speed.
- **Aether:** resonance, linking, stable transmission.
- **Void:** entropy, consumption, destabilization.

### Form runes

- **Lance:** a traveling spell body; strongest general interception tool.
- **Ward:** a durable blocking construct whose profile determines resilience and side effects.
- **Orbit:** a familiar that repeatedly emits reduced copies of its profile.
- **Burst:** a delayed event placed deeper in a lane.
- **Beam:** a near-immediate piercing channel that can strike a construct or caster and interact with a node.
- **Glyph:** a persistent mid-field inscription that attunes a ley node and modifies traffic through its radius.

### Ordered clauses

Clauses operate on the compiled program, so they can modify every element sequence and form.

1. **Echo:** schedules a reduced second execution.
2. **Fork:** creates reduced adjacent-lane branches.
3. **Anchor:** trades speed for size, stability, durability, and persistence.
4. **Seek:** retargets once near mid-field based on hostile pressure.
5. **Relay:** uses charged allied ley nodes to route the spell into another lane.
6. **Trigger:** stores the rest of the sentence as a trap and releases it when hostile traffic enters.
7. **Bind:** merges the profile into an allied spell or construct; if none exists, a temporary waiting enchantment remains.
8. **Consume:** sacrifices an allied field or construct in the chosen lane, merging its profile and converting its remaining value into power.

Clause order is retained in the program and surfaced in UI. Some effects are structurally resolved before deployment—Consume, Bind, and Trigger—while Echo and Fork schedule later executions and Anchor, Seek, and Relay alter the resulting entities.

## Runtime interaction model

Every spell entity carries a profile. Interactions compare profiles at runtime.

### Persistent modification

- Allied fields gradually add their profile to passing projectiles and can accelerate or reinforce them.
- Hostile wet or cold fields slow travel; entropy erodes damage.
- Bound spells merge the enchanter profile into the target and increase size, damage, health, and remaining lifetime according to stability.
- Consume removes a persistent allied object and adds a large fraction of its channels to the new spell.
- Ley nodes accumulate a blended profile from Glyphs and passing spells.

### Reactions

- **Steam Veil:** heat and moisture create an obscuring/damping field.
- **Thermal Shock:** heat and cold destroy stability and can burst both spell bodies.
- **Conduction:** charge follows moisture and can chain into adjacent lanes.
- **Molten Field:** heat and mass leave a damaging persistent field.
- **Blizzard:** cold and impulse create a slowing field.
- **Shatter:** mass, cold, and impulse generate lane-spreading shards.
- **Null Flux:** aether and entropy erase or heavily weaken both effects.
- **Corrosion:** entropy combined with moisture/cohesion creates construct-eating fields.
- **Resonance:** otherwise comparable forces partially cancel or the stronger body survives at reduced power.

## Arena rules

- **Astral Court:** neutral, coherent node profiles; ley charge persists longer and favors resonance/linking strategies.
- **Ember Vault:** ambient heat and volatility empower ignition and molten residue.
- **Tidal Archive:** ambient moisture improves conduction and slows projectile travel.
- **Shattered Crown:** ambient impulse and volatility strengthen forked pressure and shatter behavior.

These are match rules rather than cosmetics: the same executable can behave differently in each arena because the compiler and node base profiles receive the arena channel.

## Executable economy

The Grimoire is an unlimited ordered library. Four entries are visible in combat at once, with paging controls for quick selection.

A saved executable loads all writing stages and moves directly to lane selection. This convenience costs:

- one Ink;
- 10–25% extra mana depending on artifact;
- a per-program cooldown.

Manual glyph accuracy restores Ink. Therefore a deck is a tactical vocabulary, not a replacement for live composition.

## Artifacts

- **Prism Lens:** later element slots retain more weight.
- **Ashen Quill:** accurate manual glyphs restore more Ink.
- **Aegis Bell:** wards and orbiting constructs gain durability and support pulses.
- **Ley Key:** nodes charge faster and Relay costs less.
- **Mnemonic Crown:** executable surcharge is reduced, but manual Ink recovery is weaker.

## AI readability

The rival builds the same program structure. Its target lane appears immediately, elemental sigils are progressively revealed, then form and clauses become readable before resolution. Difficulty changes syntax density, channel duration, accuracy, and recovery—not hidden projectile speed.

## UX and visual language

The combat composer is a single staged panel:

1. Draw up to four sigils on a large canvas.
2. Select one form from six cards.
3. Select up to three ordered clauses; order badges remain visible.
4. Select a lane with live ley ownership shown.
5. Review the compiled profile, resource cost, lane, and reaction identity, then press Cast.

The visual direction uses near-black ink, charcoal metal, warm gold linework, restrained ivory text, arena-specific accents, elemental neon, large readable sans-serif labels, and procedural arcane geometry. Spell bodies use trails, radial bloom, profile pips, rotating inscriptions, impact rings, and reaction labels to make state changes visible.

## Scope and next steps

Version 1.4 implements the expanded compiler, persistent arena state, unlimited executable library, revised AI grammar, and complete procedural UI/VFX pass. Future work should add deterministic simulation tests, real network transport, accessible glyph alternatives, saved library reordering/naming, sound design, and authored character/archetype progression.

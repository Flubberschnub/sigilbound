# Sigilbound v2.0 — The Living Weave

## Design objective

Sigilbound should feel like a real-time card battler whose cards are partially written during play. The player does not merely choose an attack; they author a small executable sentence, deploy it into one of three simultaneous lane contests, and then exploit or alter the persistent state left behind by previous spells — and by the arena itself.

The v2.0 redesign has five pillars:

1. **Readable authorship:** the input flow always reads Sigils → Form → Clauses → Lane → Cast.
2. **Compositional depth:** elemental properties and generic clauses combine through shared rules rather than a list of handcrafted pair recipes.
3. **A living board:** nodes, fields, traps, constructs, enchantments, rifts, and auras persist long enough for one spell to set up another — and spells can target, curse, drain, or erase each other after they resolve.
4. **An arena that fights back:** lane traits, ambient channels, and periodic arena events make positioning a real decision independent of enemy pressure.
5. **Prepared playstyles:** an unlimited executable library lets players construct repeatable loops, while Ink, mana surcharge, and cooldowns keep manual writing relevant.

## Influences

- **Outward** — the environment is a spell component; ambient conditions change what a cast does.
- **Magicka 2** — free element stacking with emergent combinations rather than recipe tables.
- **Tyranny** — spells as sentences: core, expression, accent, gated by grammar.
- **Magic: the Gathering** — permanents, enchantments, curses, removal, sacrifice, and theft: everything on the board can modify everything else.

## Duel topology

The arena has three lanes. Every lane contains a ley node at mid-field and a **lane trait** rolled at the start of each duel. A lane therefore contains several overlapping contests:

- spell versus spell interception;
- construct versus pressure;
- control of the ley node;
- persistent field ownership;
- trap, hex, and trigger positioning;
- rift geometry and relay routes;
- exploiting (or suffering) the lane trait and arena events.

A projectile that passes through an allied node absorbs part of the node profile and gains force. A hostile node weakens it and can produce a reaction. Glyph forms claim and tune nodes. Relay clauses route a spell through an allied charged node into a more useful lane. Rifts throw hostile projectiles into other lanes entirely. This turns the three lanes into a small programmable board.

### Lane traits

Rolled per duel (three distinct traits from six), shown in the lane picker and etched at the top of each lane:

- **Ley Current:** projectiles and surges travel faster.
- **Bedrock:** constructs deploy with more durability.
- **Wellspring:** owning the lane's node grants steady mana.
- **Wild Magic:** reactions hit harder and fields last longer.
- **Ashen Ground:** fields persist longer.
- **Thin Veil:** the node charges faster.

### Arena events

Every 11–16 seconds the arena intervenes, announced with a banner:

| Arena | Event | Effect |
|---|---|---|
| Astral Court | Ley Surge | a random node flares with charge |
| Ember Vault | Vent Eruption | neutral molten hazard field erupts in a lane |
| Tidal Archive | Tide Surge | all projectiles briefly slowed and soaked |
| Shattered Crown | Fracture | a shatter hazard field tears open in a lane |
| Verdant Reach | Bloom | a regrowth field mends **all** constructs in a lane |
| Radiant Basilica | Light Shaft | spells in one lane gain damage and radiance |

Hazard fields are owned by the environment: hostile to both duelists, and legitimate targets for Surge damping, Siphon draining, and positional play.

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
- **Radiance:** revelation, searing light, cleansing order.
- **Verdance:** growth, regrowth, entanglement.

### Form runes

- **Lance:** a traveling spell body; strongest general interception tool.
- **Ward:** a durable blocking construct whose profile determines resilience and side effects.
- **Orbit:** a familiar that repeatedly emits reduced copies of its profile.
- **Burst:** a delayed event placed deeper in a lane.
- **Beam:** a near-immediate piercing channel that can strike a construct or caster and interact with a node.
- **Glyph:** a persistent mid-field inscription that attunes a ley node and modifies traffic through its radius.
- **Surge:** a wide, slow wavefront that shoves hostile projectiles backward and damps hostile fields — a pure tempo and positioning tool.
- **Rift:** a portal at the lane's forward third. Hostile projectiles entering it are thrown into another lane; allied projectiles pass through faster and tinted. Three redirects, then collapse.
- **Aura:** a global enchantment standing beside the caster. Every allied cast inherits a share of the aura profile while it survives. One per duelist; recasting replaces it. Fragile, and a legal target for beams, hexes, consume, and dispel.

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
9. **Swift:** the tempo mirror of Anchor — faster and leaner at a small cost to size and damage.
10. **Siphon:** inverts friction. Hostile fields and nodes the spell crosses are drained into it, and construct hits leech mana back to the caster.
11. **Hex:** the hostile mirror of Bind. Curses the strongest hostile persistent object in the lane, sapping its damage, durability, and lifetime; with no target, a waiting hex curses the next hostile arrival.
12. **Dispel:** the removal instant. On spell-to-spell contact the hostile body is erased outright (consuming the dispel charge), and hostile traps, fields, and enchantments are torn up in passing.

Clause order is retained in the program and surfaced in the UI. Some effects are structurally resolved before deployment — Consume, Hex, Bind, and Trigger — while Echo and Fork schedule later executions and Anchor, Swift, Seek, Siphon, Dispel, and Relay alter the resulting entities.

## Runtime interaction model

Every spell entity carries a profile. Interactions compare profiles at runtime.

### Persistent modification

- Allied fields gradually add their profile to passing projectiles and can accelerate or reinforce them.
- Hostile wet, cold, or entangling fields slow travel; entropy erodes damage; molten hazards scorch.
- A standing aura blends into every allied cast.
- Bound spells merge the enchanter profile into the target and increase size, damage, health, and remaining lifetime according to stability.
- Hexed objects lose damage, durability, and lifetime, and carry visible curse geometry.
- Consume removes a persistent allied object and adds a large fraction of its channels to the new spell.
- Siphon drains hostile fields and node charge into the passing spell.
- Dispel erases hostile spell bodies, traps, fields, and enchantments outright.
- Ley nodes accumulate a blended profile from Glyphs and passing spells.
- Rifts rewrite projectile geometry between lanes.

### Reactions

- **Steam Veil:** heat and moisture create an obscuring/damping field.
- **Thermal Shock:** heat and cold destroy stability and can burst both spell bodies.
- **Conduction:** charge follows moisture and can chain into adjacent lanes.
- **Molten Field:** heat and mass leave a damaging persistent field.
- **Blizzard:** cold and impulse create a slowing field.
- **Shatter:** mass, cold, and impulse generate lane-spreading shards.
- **Null Flux:** aether and entropy erase or heavily weaken both effects.
- **Corrosion:** entropy combined with moisture/cohesion creates construct-eating fields.
- **Wildfire:** heat and growth leave a creeping burn field.
- **Overgrowth:** growth and moisture entangle spells and mend constructs.
- **Sanctify:** radiance and cohesion cleanse the survivor — entropy stripped, hexes lifted.
- **Eclipse:** radiance and entropy detonate in a blinding erasure.
- **Resonance:** otherwise comparable forces partially cancel or the stronger body survives at reduced power.

## Arena rules

- **Astral Court:** neutral, coherent node profiles; ley charge persists longer and favors resonance/linking strategies.
- **Ember Vault:** ambient heat and volatility empower ignition and molten residue.
- **Tidal Archive:** ambient moisture improves conduction and slows projectile travel.
- **Shattered Crown:** ambient impulse and volatility strengthen forked pressure and shatter behavior.
- **Verdant Reach:** ambient growth and moisture; construct sustain and overgrowth strategies thrive.
- **Radiant Basilica:** ambient radiance and cohesion; beams, sanctify shields, and eclipse burst casts are favored.

These are match rules rather than cosmetics: the same executable can behave differently in each arena because the compiler, node base profiles, and event schedule receive the arena channel.

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
- **Hexwright Ring:** Hex, Dispel, and Siphon are cheaper and strike deeper.
- **Verdant Chalice:** growth channels amplified; overgrowth healing also mends the caster.

## AI readability

The rival builds the same program structure. Its target lane appears immediately, elemental sigils are progressively revealed, then form and clauses become readable before resolution. Difficulty changes syntax density, channel duration, accuracy, and recovery — not hidden projectile speed. The rival respects aura exclusivity and answers dominant player elements with counters (Radiance answers Void, Fire answers Verdance, and so on).

## UX and visual language

The combat composer is a single staged panel with a golden diamond step-rail and a live sentence preview:

1. Draw up to four sigils on a large canvas.
2. Select one form from nine cards.
3. Select up to three ordered clauses; order badges remain visible.
4. Select a lane with live ley ownership and lane traits shown.
5. Review the compiled profile, resource cost, lane, and reaction identity, then press the pulsing Cast button.

The visual direction uses near-black lacquer, brushed charcoal metal, engraved gradient-gold borders with filigree corner ticks, restrained ivory text, arena-specific accents, elemental neon reserved for spell bodies, and procedural arcane geometry: a rotating ley-circle watermark, vignette lighting, comet trails, layered flickering beams, node charge-arc gauges, expanding shockwaves, screen shake, and pop-in reaction text.

## Scope and next steps

Version 2.0 implements the twelve-channel compiler, nine forms, twelve clauses, arena events, lane traits, spell-to-spell interaction stack (aura/hex/dispel/siphon), six arenas, seven artifacts, and a full presentation overhaul. Future work should add deterministic simulation tests, real network transport, accessible glyph alternatives, saved library reordering/naming, sound design, and authored character/archetype progression.

# Spell System Reference

## Program schema

```text
Program {
  name
  elements[1..4]  // ordered, duplicates allowed
  form             // exactly one
  clauses[0..3]    // ordered, unique in current UI
}
```

A program is valid when it has at least one element and one form. Lane is selected at deployment rather than stored in the executable, allowing the same saved program to answer different board states.

## Compilation

`SpellSystem.compile(elements, artifact, arena)` creates a continuous `Profile`.

Base positional weights:

| Position | Weight |
|---|---:|
| 1 | 1.00 |
| 2 | 0.82 |
| 3 | 0.66 |
| 4 | 0.52 |

Prism Lens adds 0.14 to every position after the first.

### Channels

Twelve channels: `heat, moisture, impulse, mass, cohesion, volatility, cold, charge, aether, entropy, radiance, growth`.

| Element | Primary contributions |
|---|---|
| Fire | heat, volatility, impulse |
| Water | moisture, cohesion, negative heat |
| Wind | impulse, volatility, negative cohesion |
| Stone | mass, cohesion, slight negative impulse |
| Frost | cold, cohesion, volatility, negative heat |
| Lightning | charge, impulse, volatility |
| Aether | aether, cohesion, charge |
| Void | entropy, volatility, negative cohesion/moisture |
| Radiance | radiance, heat, cohesion, negative entropy |
| Verdance | growth, moisture, cohesion, negative volatility |

Arena ambience is added before derived reactions are calculated. The Verdant Chalice adds a small growth bias to every compile.

### Derived profile values

Derived values are recomputed whenever profiles merge or scale:

- steam = heat × moisture above threshold
- magma = heat × mass above threshold
- shatter = cold × mass plus mass × impulse above threshold
- conduction = charge × moisture above threshold
- blizzard = cold × impulse above threshold
- null flux = aether × entropy above threshold
- corrosion = entropy × moisture/heat above threshold
- resonance = aether × cohesion/charge
- thermal shock = heat × cold above threshold
- wildfire = heat × growth above threshold
- overgrowth = growth × moisture/cohesion above threshold
- sanctify = radiance × cohesion/aether above threshold
- eclipse = radiance × entropy above threshold

These derived values do not directly choose a spell. They influence form stats, collisions, fields, node interactions, and reaction resolution.

## Form interpretation

### Lance

Creates a projectile. Impulse and charge increase speed; mass and aether increase radius; force channels increase damage. Anchor slows the projectile while making it larger and stronger; Swift does the reverse.

### Ward

Creates a blocking construct. Stability controls HP and lifetime. Aegis Bell increases both and periodically reinforces allied constructs in the lane.

### Orbit

Creates a persistent familiar with HP and lifetime. It emits projectiles at an interval reduced by impulse, charge, and the Swift clause.

### Burst

Creates a delayed deep-lane event. On detonation it damages nearby hostile constructs/caster space and emits fields, chains, or shards based on its profile.

### Beam

Creates a short-lived channel that immediately resolves against the nearest hostile entity in its lane, or the opposing caster when no entity blocks it. It also touches the ley node. Light Shaft events amplify beams cast into the consecrated lane.

### Glyph

Creates a persistent mid-field inscription. It continuously attunes the lane's ley node and modifies spell traffic within its radius.

### Surge

Creates a wide, slow wavefront. It does not trade with hostile projectiles — it shoves them backward (scaled by impulse), erodes their damage, and damps hostile fields it crosses. It washes over hostile constructs as a single hit and deals reduced caster damage on arrival.

### Rift

Creates a portal at the lane's forward third with three redirect charges. A hostile projectile entering the rift is thrown into the adjacent lane where the rift owner is strongest; allied projectiles pass through once with bonus speed and a share of the rift profile. Collapses when charges, HP, or lifetime run out.

### Aura

Creates a global enchantment beside the caster (one per duelist; recasting replaces it). While it stands, every allied cast blends 35% of the aura profile into its compiled profile. The aura is a construct: it can be attacked, bound, hexed, consumed, or dispelled.

## Clause resolution

### Pre-deployment clauses

- **Consume:** find the best allied persistent object in the lane (glyph, field, ward, orbit, rift, or aura), merge 58% of its profile, convert remaining value to power, and destroy it.
- **Hex:** curse the strongest hostile persistent object in the lane — blending entropy pressure into it and cutting its damage, HP, and lifetime. With no target, an eight-second waiting hex curses the next hostile construct, rift, or projectile entering the lane.
- **Bind:** merge into the nearest valid allied spell/construct. When none exists, create an eight-second waiting enchantment.
- **Trigger:** create a trap containing the sentence with Trigger removed. Hostile projectile/beam/shard proximity releases the stored program. A hostile Dispel spell destroys the trap without triggering it.

### Entity clauses

- **Anchor:** adjust speed, size, HP, force, and lifetime according to form.
- **Swift:** the tempo mirror — more speed and cadence, slightly less size and damage.
- **Seek:** once near mid-field, choose the lane with highest hostile pressure.
- **Relay:** on crossing a friendly charged node, absorb its profile and route to another useful allied-node lane.
- **Siphon:** hostile fields crossed are drained into the spell (profile, damage) instead of weakening it; hostile nodes lose extra charge into the spell; construct hits leech mana to the caster. Amplified by the Hexwright Ring.
- **Dispel:** on spell-to-spell contact the hostile body is destroyed outright and the dispel charge is consumed (mutual dispels annihilate). Hostile fields, traps, and enchantments crossed are erased. Adds bonus damage against constructs.

### Scheduled clauses

- **Fork:** schedule reduced executions in valid adjacent lanes.
- **Echo:** schedule a reduced execution in the original lane after a delay.

## Ley nodes

Each lane has a `LeyNode`:

```text
LeyNode {
  owner: neutral | player | rival
  charge: 0..100
  profile
}
```

Glyphs continuously attune nodes. Passing spells can claim neutral nodes. Friendly nodes blend into and empower passing projectiles. Hostile nodes compare profiles, weaken the passing body, and may produce a visible reaction. A Relay spell can spend node charge to move into another lane. Thin Veil lanes charge nodes 40% faster; Wellspring lanes pay their owner mana.

## Lane traits

Three distinct traits are rolled per duel from:

| Trait | Effect |
|---|---|
| Ley Current | projectiles and surges +18% speed |
| Bedrock | constructs +30% HP on deploy |
| Wellspring | owned node grants ~1.7 mana/s |
| Wild Magic | reaction damage ×1.2, reaction strength ×1.35, fields +20% life |
| Ashen Ground | fields +40% life |
| Thin Veil | node attunement ×1.4 |

## Arena events

Fired every 11–16 seconds (suppressed during the practice grace period). Hazard fields use the environment owner (2) and treat both duelists as hostile:

| Arena | Event |
|---|---|
| Astral Court | Ley Surge — random node gains 16–30 charge |
| Ember Vault | Vent Eruption — molten hazard field (heat/mass) in a random lane |
| Tidal Archive | Tide Surge — 3.5 s global slow + moisture soak on projectiles |
| Shattered Crown | Fracture — shatter hazard field (mass/impulse/cold) in a random lane |
| Verdant Reach | Bloom — regrowth field that mends all constructs in its radius |
| Radiant Basilica | Light Shaft — 4.5 s lane consecration: +damage and radiance for spells inside |

## Runtime entity interactions

Every projectile, construct, field, trap, enchantment, rift, and aura retains its Profile. This allows later effects to modify earlier effects without knowing their exact element recipe.

Examples:

- Fire–Aether Lance passes through an allied Water–Lightning Glyph: it gains moisture/charge, potentially changing its dominant reaction to conduction or steam.
- A Radiance–Aether Aura stands beside your caster: every subsequent cast gains radiance and cohesion, pushing collisions toward Sanctify outcomes.
- A Void Hex lands on the rival's Stone Ward: the ward's damage, HP, and lifetime drop, and its profile drifts toward corrosion vulnerability.
- A Void–Lightning Lance with Dispel meets the rival's empowered projectile: the projectile is simply erased, no reaction rolled.
- A Siphon Lance crosses the rival's steam field and hostile node: it drinks both, arriving heavier than it left.
- Consume sacrifices your own Aura before casting a Fire Burst: the merged profile inherits the aura's channels and converts its remaining value into power.
- A hostile projectile enters your Rift at mid-lane: it is thrown into the lane your ward already guards.

## Serialization and library

Programs serialize as:

```text
name|ELEMENT,ELEMENT|FORM|CLAUSE,CLAUSE
```

The SharedPreferences library uses a count plus indexed entries and has no hard slot limit. Cooldowns are runtime-only and resize to match the saved library. v1.4 saves load unchanged: new enum constants are appended, and stored selection ordinals remain valid.

## Balance levers

- element position weights;
- form base mana costs;
- clause costs;
- executable surcharge and Ink economy;
- per-executable cooldown;
- arena ambient channels and event cadence;
- lane trait multipliers;
- node charge decay and relay spending;
- aura inheritance share (0.35) and hex erosion factors;
- form-specific profile multipliers;
- AI syntax size, channel duration, and recovery.

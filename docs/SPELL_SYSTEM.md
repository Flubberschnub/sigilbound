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

Arena ambience is added before derived reactions are calculated.

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

These derived values do not directly choose a spell. They influence form stats, collisions, fields, node interactions, and reaction resolution.

## Form interpretation

### Lance

Creates a projectile. Impulse and charge increase speed; mass and aether increase radius; force channels increase damage. Anchor slows the projectile while making it larger and stronger.

### Ward

Creates a blocking construct. Stability controls HP and lifetime. Aegis Bell increases both and periodically reinforces allied constructs in the lane.

### Orbit

Creates a persistent familiar with HP and lifetime. It emits projectiles at an interval reduced by impulse and charge.

### Burst

Creates a delayed deep-lane event. On detonation it damages nearby hostile constructs/caster space and emits fields, chains, or shards based on its profile.

### Beam

Creates a short-lived channel that immediately resolves against the nearest hostile entity in its lane, or the opposing caster when no entity blocks it. It also touches the ley node.

### Glyph

Creates a persistent mid-field inscription. It continuously attunes the lane’s ley node and modifies spell traffic within its radius.

## Clause resolution

### Pre-deployment clauses

- **Consume:** find the best allied persistent object in the lane, merge 58% of its profile, convert remaining value to power, and destroy it.
- **Bind:** merge into the nearest valid allied spell/construct. When none exists, create an eight-second waiting enchantment.
- **Trigger:** create a trap containing the sentence with Trigger removed. Hostile projectile/beam/shard proximity releases the stored program.

### Entity clauses

- **Anchor:** adjust speed, size, HP, force, and lifetime according to form.
- **Seek:** once near mid-field, choose the lane with highest hostile pressure.
- **Relay:** on crossing a friendly charged node, absorb its profile and route to another useful allied-node lane.

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

Glyphs continuously attune nodes. Passing spells can claim neutral nodes. Friendly nodes blend into and empower passing projectiles. Hostile nodes compare profiles, weaken the passing body, and may produce a visible reaction. A Relay spell can spend node charge to move into another lane.

## Runtime entity interactions

Every projectile, construct, field, trap, and enchantment retains its Profile. This allows later effects to modify earlier effects without knowing their exact element recipe.

Examples:

- Fire–Aether Lance passes through an allied Water–Lightning Glyph: it gains moisture/charge, potentially changing its dominant reaction to conduction or steam.
- A Void Bind is placed before a Stone Ward exists: a waiting enchantment remains; the later Ward receives entropy and may become a corrosion source.
- Consume sacrifices a Frost Glyph before casting a Fire Burst: the merged profile gains thermal shock and can resolve differently from the same Burst without the sacrifice.
- Relay routes a charged Aether projectile through a controlled node to reinforce a weak adjacent lane.

## Serialization and library

Programs serialize as:

```text
name|ELEMENT,ELEMENT|FORM|CLAUSE,CLAUSE
```

The SharedPreferences library uses a count plus indexed entries and has no hard slot limit. Cooldowns are runtime-only and resize to match the saved library.

## Balance levers

- element position weights;
- form base mana costs;
- clause costs;
- executable surcharge and Ink economy;
- per-executable cooldown;
- arena ambient channels;
- node charge decay and relay spending;
- form-specific profile multipliers;
- AI syntax size, channel duration, and recovery.

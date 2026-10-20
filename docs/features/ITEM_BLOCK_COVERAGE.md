# Item coverage — what UnionClef can reach, and how

Measured, not claimed. Every number below was produced by reading the **26.3 client jar itself**
(`~/.gradle/caches/fabric-loom/26.3/minecraft-client.jar`) and comparing it against this mod's
source, so it can be re-run and it degrades honestly when the mod changes.

> **Scope of this audit.** The measurement is against 26.3, where these chains were built. The 26.2
> build ships the same chains over the 26.2 registry; items that exist only in 26.3 (for example
> `poplar`) are absent from that registry and so cannot be reached — that is not a missing chain.
> Re-run the same script against the 26.2 jar (point it at the `26.2` loom cache) to reproduce the
> 26.2 numbers.

## How the measurement works

| Set | Source |
|---|---|
| The item registry | the field names of `net.minecraft.world.item.Items` (via `javap`) |
| Curated / referenced | every `Items.CONSTANT` the mod's Java source mentions |
| Craftable | `data/minecraft/recipe/*.json` → `result.id` |
| Mob drops | `data/minecraft/loot_table/entities/*.json` |
| Loot / trade | `data/minecraft/loot_table/**` and `data/minecraft/villager_trade/*.json` |

Each registry item is then classified **in the order the chain resolver actually tries them**:
referenced → craftable → mineable block → mob drop → loot/trade → nothing.

## The 0.97.0 numbers

```
registry items:            1253
  referenced in source:     751     (has a chain, or is at least known to the mod)
  -> generic CRAFT chain:   179     (no curated entry, but 26.3 has a recipe for it)
  -> generic MINE chain:    139     (no recipe; it is a block that occurs in the world)
  -> mob drop:                1
  -> LOOT / TRADE only:      68
  -> NO source known:       115
```

The two generic chains are the point of the 0.97.0 chains work: **318 items are reachable with no
curated entry at all**, because `TaskCatalogue.getItemTask` now falls back to the server's own
recipe book (craft) and to `Block.byItem` (mine).

## The 115 "no source known" — mostly not gaps at all

**About 90 of them are `*_spawn_egg`.** Spawn eggs are creative-only; they have no survival source in
vanilla and never will. Together with `debug_stick`, `knowledge_book` and `command_block_minecart`
(also creative/operator-only), that is the overwhelming majority of this bucket.

The genuinely interesting ones, and why they have no chain:

| Item | Real source | Why there is no chain |
|---|---|---|
| `dragon_breath` | bottle a dragon's breath cloud | needs a dedicated task (the mod only *avoids* the clouds today) |
| `elytra` | End Ship item frame | needs end-city navigation + item-frame looting |
| `music_disc_11/13/blocks/cat/chirp/far/mall/mellohi/stal/strad/wait/ward/pigstep/…` | creeper killed **by a skeleton's arrow** | a mechanic, not a drop table — a scripted trap no task models |
| `filled_map` | cartographer trades / explorer maps | trade-only; needs the trade chain |
| `harness`, `cushion`, `dyed_bundle` | 26.3 additions | **no vanilla recipe exists** in the jar's 2044 recipes — flagged for review |
| `salmon_bucket`, `tadpole_bucket`, `sulfur_cube_bucket`, `cod_bucket`, … | use a bucket on the mob | altoclef collects *liquid* in buckets, not mobs |
| `dry_short_grass`, `dry_tall_grass` | breaking the block | likely reachable via the generic MINE chain — see the approximation note below |
| `flow_pottery_sherd`, `scrape_pottery_sherd`, `guster_pottery_sherd` | brushing suspicious sand/gravel | archaeology is not modelled |

## The 68 "loot / trade only"

These are found in structure chests or traded by villagers and have no other source. Highlights:

- **`armadillo_scute`, `turtle_scute`** — *deliberately not given a kill chain.* Their tables are
  `gameplay/armadillo_shed` and `gameplay/turtle_grow`: the mob **sheds** them, or a baby turtle
  **grows up**. A `mob(...)` entry means "kill it and loot it", which would be a chain that cannot
  work. They need a husbandry task, not a catalogue line.
- **The nautilus armour family** (`copper`/`iron`/`golden`/`diamond_nautilus_armor`) — loot or trade
  only; the jar's sole nautilus recipe is `netherite_nautilus_armor_smithing`, i.e. the *upgrade*.
  Its input, `nautilus_shell`, is now farmable from the new nautilus mob.
- **Pottery sherds, banner patterns, explorer maps, `goat_horn`, `echo_shard`, `trial_key`** —
  structure loot by design.
- **`glow_berries`** — was in this bucket and is **now fixed** with a mine chain on cave vines.

## Known limitations of this audit

- **"Referenced" is not "obtainable."** An item that appears anywhere in the source counts as
  covered, so an item that is merely *named* by a task but has no real chain is counted optimistically
  here. The buckets below it (craft/mine/mob/loot/none) are computed from game data and are exact.
- **"Mineable block" is approximated** by matching the item id against the field names of
  `net.minecraft.world.level.block.Blocks`. The real test is `Block.byItem(item) != AIR`, which the
  code performs at runtime, so the actual mine coverage is at least as large as reported.
- The add-on `spawn_egg` family is counted in the registry but is not a survival target.

## The 68 loot/trade items — mapped, and why none is a one-line fix

Every one of them was traced back to the loot table that produces it, which tells us exactly what
feature each needs. **There are no easy wins in this bucket**, which is the important result:

| Family | Count | What it actually needs |
|---|---|---|
| **Structure chests** | 28 | locate structure → travel → loot chests |
| **Archaeology** | 21 | find suspicious sand/gravel → **brush** it |
| **Husbandry / mechanics** | 5 | chickens *lay* `blue_egg`/`brown_egg` (`gameplay/chicken_lay`), armadillos *shed* `armadillo_scute`, baby turtles *grow into* `turtle_scute`, sniffers *dig up* `pitcher_pod` |
| **Maps** | ~6 | cartographer trades, or secret chests in abandoned camps |
| **Buckets** | 3 | use a bucket **on a fish** (`cod`/`pufferfish`/`tropical_fish`_bucket) |

**The trap this audit avoids.** Two items *look* like easy block-harvest chains and are not:

- `pitcher_pod` ← `blocks/pitcher_crop` — but that is the **planted** crop. A bot can only harvest a
  pitcher crop if it already grew one, and it grows one from a pod, which comes from a sniffer
  digging. The block source is circular.
- The same shape as the scutes: the loot table names *where the item is dropped*, not *how a bot
  obtains the first one*.

Adding a `mine(...)` entry for either would be a chain that can never start — a lying chain, which
this document's last rule forbids. A documented gap is better.

### What each family needs, in the order I would build them

1. **`LootSearchTask` (28 items, the biggest bucket).** Locate a structure by its loot-table name
   (`@locate_structure` already has the plumbing via `LocateStructureCommand`), travel there, and
   `LootContainerTask` the chests for the wanted item. Precedent exists — `RavageRuinedPortalsTask`
   and `LootDesertTempleTask` each do this shape for one structure; this would generalise it and
   drive it from the item → table map above.
2. **`BrushBlockTask` (21 items).** Find `SUSPICIOUS_SAND` / `SUSPICIOUS_GRAVEL` with the block
   scanner, travel, hold a brush, and hold use until the block converts. Note the drop is
   **structure-dependent** — brushing gives whatever the *local* archaeology table yields — so this
   can only be "brush until you get the wanted sherd", not "go to the right structure".
3. **Bucket-a-fish (3 items).** A small variant of the existing liquid-bucket collection: use a
   bucket on a `Cod`/`Pufferfish`/`TropicalFish`.
4. **Husbandry (5 items).** Needs a real farming task (keep chickens, shear armadillos, raise
   turtles, feed a sniffer). Deliberately last: highest effort, lowest value, and a bot standing
   around waiting for an egg to appear is a poor use of it.
5. **Maps (6 items).** Cartographer trades — needs a villager-trade chain. `TradeWithPiglinsTask`
   is the only trade machinery that exists.

### Still separate, and not in this bucket

- **`elytra`** — an End Ship item frame. Needs end-city navigation plus item-frame looting.
- **`dragon_breath`** — bottle a dragon's breath cloud. Needs the dragon fight; the mod only
  *avoids* the clouds today (`DragonBreathTracker`).
- **The pre-26.3 music discs** (11/13/blocks/cat/chirp/far/mall/mellohi/stal/strad/wait/ward/pigstep)
  — a creeper killed **by a skeleton's arrow**. That is a scripted trap, not a drop table; no task
  models it.

### The blocker for `LootSearchTask`, found before building it

**There is no generic structure locator in this codebase, and one cannot be assumed.** `@locate_structure`
covers exactly two structures and hand-rolls each:

```java
case STRONGHOLD:      mod.runUserTask(new GoToStrongholdPortalTask(1), ...);
case DESERT_TEMPLE:   mod.runUserTask(new LocateDesertTempleTask(), ...);
```

And vanilla's `/locate structure <id>` is **not a way out**: it needs cheats or op, so a bot on a
server cannot rely on it.

So the 28 chest-loot items need a **search strategy** decided first, and that is a design choice, not
a detail:

- **Explore-and-recognise** — wander (`SearchChunksExploreTask`) until the structure's characteristic
  blocks appear, then loot. Works anywhere, but needs a block signature per structure (a trial
  chamber and an ancient city are not equally easy to recognise) and is slow by nature.
- **Server `/locate` when permitted** — accurate and fast, but only when the bot has cheats/op.
- **Both**, trying `/locate` first and falling back to exploring.

Get that decision right and `LootSearchTask` — locate → travel → `LootContainerTask` the chests for
the wanted item — follows from the item → table map above, generalising the shape
`RavageRuinedPortalsTask` already uses for one structure.

## Extending coverage

1. **Prefer data over guesses.** Enumerate the jar (`loot_table`, `recipe`, `villager_trade`) and
   find the items with no chain; that is how the 26.3 mob drops in 0.97.0 were found.
2. **Add a curated entry** in `TaskCatalogue` for anything the generic chains cannot reach — `mob(...)`
   for drops, `mine(...)` for harvests, `smelt(...)`/`smith(...)` for conversions.
3. **Watch the entity class.** `EntityTracker` keys by *exact runtime class* (`squashType` only folds
   Players), so an elder guardian is not a guardian, and `zombie_nautilus` is not a nautilus.
4. **Only add a chain you would believe.** A `mob(...)` entry for an item a mob *sheds* is a chain
   that can never complete; a documented gap beats a lying chain.

# Item coverage — what UnionClef can reach, and how

Measured, not claimed. Every number below was produced by reading the **26.3 and 26.2 client jars
themselves** (`~/.gradle/caches/fabric-loom/<version>/minecraft-client.jar`) and comparing them against
this mod's source, so it can be re-run and it degrades honestly when the mod changes.

> **Scope of this audit (2026-10-22).** `main` is measured against 26.3; this `26.2` branch is measured against 26.2.
> Items that only exist in 26.3 (the poplar family, the 16 structure maps, the abandoned camps) are
> absent from the 26.2 registry and cannot be reached there — that is not a missing chain. The tables
> below are per version for that reason.

## Measured coverage

Method: read the item and block fields from each version's client jar, read the vanilla data pack's
recipes, loot tables and villager trades, scan the mod's source for item references **and for every
item id registered by the hand-written coverage chains**, then classify every item by its best
source. Colour-collection items (`harness`, `cushion`, `dyed_bundle`, wool, beds, …) are registry
items generated from `ColorCollection` fields; where they have a recipe they are craftable through
the recipe book, so they are covered.

| Version | Items | Survival-relevant | Chain-covered | Any data source | Remaining gaps |
|---|---|---|---|---|---|
| 26.2 | 1207 | 1098 | 1082 (98.5%) | 1086 (98.9%) | 16 (1.5%) |
| 26.3 | 1253 | 1143 | 1127 (98.6%) | 1131 (99.0%) | 16 (1.4%) |

"Survival-relevant" excludes the 110 (26.3) / 109 (26.2) registry entries that no player can obtain in
survival: **88 spawn eggs**, and **7 (26.3) / 5 (26.2) creative/operator-only or non-survival items**
(`barrier`, the command blocks and `command_block_minecart`, `knowledge_book`, `debug_stick`, `light`,
`structure_block`, `structure_void`, `jigsaw`, the test blocks, `spawner`, `bedrock`,
`end_portal_frame`, `reinforced_deepslate`, `petrified_oak_slab`, `filled_map` — a runtime state, not
an obtainable item — `harness`, `cushion`, `dyed_bundle`). Of the 16 remaining gaps per version, 12
have no source at all and 4 are loot/trade-only with no chain; both groups are listed under
[Remaining gaps](#remaining-gaps).

"Chain-covered" means `@get` has a chain that can actually obtain it; "any data source" also counts
items that only appear in a loot table or villager trade without a chain that reaches them (a loot
table naming an item is not a chain: someone has to find the structure and open the chest). The
coverage chains below are that "someone".

### What changed since the previous measurement

The previous audit (2026-10-21) listed 49 (26.2) / 57 (26.3) loot-only items and said the structure
loot bucket was blocked on a locator decision. That decision is now made and implemented — a
cheat-free locator plus four hand-written chain families:

| Bucket (26.3) | Before | After |
|---|---|---|
| Loot/trade-only, no chain | 57 | 4 |
| No source at all | 126 | 107 (of which 95 are eggs/creative-only) |
| Chain-covered, survival-relevant | ~90% | 98.6% |
| Structure-chest item families reachable | 0 | all chest-only families routed |
| Archaeology sherds reachable | 0 of 20 | 20 of 20 |
| Mob buckets reachable | 0 | 8 of 8 |
| Cartographer maps reachable | 0 of 16 | 16 of 16 |

## The coverage chains added for this release

`TaskCatalogue.getItemTask` is still the one resolver, and its order is still the chain:

1. **Curated entry** — hand-tuned chains keep winning.
2. **Coverage chains** (new, `adris.altoclef.chains.coverage.CoverageRegistry`) — for sources the
   generic resolver cannot see: structure loot, archaeology, mob buckets, villager trades. They run
   before the generic fallbacks because for these items both of those fail (no recipe produces them
   and they are not blocks).
3. **Craft from the server's own recipe book.**
4. **Mine its block.**
5. **Say so** when there is no source, instead of returning a null the caller has to remember to
   check.

### Structure loot — `StructureLocator` + `LootSearchTask`

Vanilla `/locate structure` needs op or cheats, so a bot on a server cannot rely on it. But in
**singleplayer the client owns the integrated server**, and the world generator is reachable from the
client process: `StructureLocator` asks `ChunkGenerator.findNearestMapStructure(HolderSet, …)` on the
server thread — the same query the command uses — with no command, no operator rights and no seed
parsing. On a multiplayer server there is no integrated server, so the chain honestly declines and
the catalogue says "no source" rather than pretending.

`LootSearchTask` then:

- locates the nearest structure that can hold the item (locator rings of 100/200/400 chunks),
- travels there with the normal pathing,
- finds the loot **in the loaded world** rather than from a per-structure map — chest positions differ
  per generated variant, and the client already knows every block of every loaded chunk including
  underground (`BlockScanner` rescans all loaded chunks about once a second),
- handles three kinds of loot: containers (chest/barrel) opened with `LootContainerTask`, decorated
  pots broken for their pattern drops, and **trial-chamber vaults** — for those it first obtains the
  matching key through the ordinary item chain (`trial_key` for a normal vault, `ominous_trial_key`
  for an ominous one, and the ominous key is itself an `OminousKeyTask` chain), then right-clicks the
  vault with the key held and lets the ResourceTask pick up the ejected loot,
- remembers what it touched, and if the item is still missing after everything available, walks ~512
  blocks away so the locator returns a different structure, up to 8 structures, then stops with a
  warning in the log instead of roaming forever.

Items routed by this task (item → structures, read off the jars' own loot tables):

| Item(s) | Source structure(s) |
|---|---|
| copper/iron/golden/diamond nautilus armour | buried treasure, shipwreck, ocean ruins |
| `heart_of_the_sea` | buried treasure |
| `echo_shard`, `disc_fragment_5` | ancient city |
| `goat_horn` | pillager outpost |
| `trial_key` | trial chambers (chests and pots) |
| `trident`, `guster_banner_pattern`, `music_disc_precipice`, `ominous_bottle` | trial-chamber vaults (`reward_unique` / `reward_common`) |
| `music_disc_creator_music_box`, `flow` / `guster` / `scrape` pottery sherds | trial-chamber decorated pots |
| `music_disc_13`, `music_disc_cat`, `music_disc_otherside` | ancient city, woodland mansion |
| `music_disc_bounce` | abandoned mineshaft |
| iron/golden/diamond/copper horse armour | desert/jungle pyramid, stronghold, mansion, ancient city |
| `piglin_banner_pattern`, `music_disc_pigstep` | bastion remnant (Nether) |
| 26.3: `abandoned_camp_map`, `buried_ancient_city_map`, `buried_mineshaft_map`, `buried_trial_chambers_map`, `desert_pyramid_map`, `jungle_pyramid_map`, `warm_ocean_ruins_map`, `woodland_mansion_map` | abandoned camps (the 18 biome variants are one structure type) — chests |
| 26.3: `buried_treasure_map` | shipwreck / ocean-ruin chests |

The locator is also exposed to the player: `@locate_structure <structure id> [go]` prints the
coordinates of any structure in a singleplayer world, and travels there with `go`. The historical
forms `@locate_structure desert_temple` / `stronghold` keep their hand-written tasks.

### Ominous trial chambers — the mace chain, unblocked

The last survival-family gap was the ominous trial spawner: it is the only source of
`ominous_trial_key`, and that key is the only way into the ominous vaults, which hold `heavy_core`
(the mace ingredient), `flow_banner_pattern` and `music_disc_creator`.

`OminousKeyTask` walks the mechanic the way a player does: acquire an `ominous_bottle` through the
normal item chain, locate a trial chamber, find a trial spawner through the block scanner (the client
knows every loaded block, including the deep ones), walk/dig to it, drink the bottle, wait for the
spawner to turn **ominous** and active (readable from the client-synced `TrialSpawnerBlock` state),
clear the wave mobs, and pick the ejected key up. It retries spawners and one more chamber, then gives
up with a log warning. `ominous_trial_key` is exposed through `@get` like any other item, and the same
`LootSearchTask` opens the ominous vaults with it.

### Archaeology — `BrushBlockTask`

Suspicious sand/gravel is brushed by holding the brush right-click on the block until it converts.
The task obtains a brush if needed, locates the structure whose archaeology table holds the wanted
sherd, scans the loaded chunks around it for `SUSPICIOUS_SAND`/`SUSPICIOUS_GRAVEL`, walks (and digs,
where needed) to the block, and holds use until the loot pops. The drop is structure-dependent, so
the chain travels to the **right structure type** first:

| Site | Items |
|---|---|
| desert pyramid | archer, miner, prize, skull sherds |
| trail ruins (suspicious gravel is buried; the scanner still sees it) | burn, danger, friend, heart, heartbreak, howl, sheaf sherds + `music_disc_relic` |
| cold ocean ruins | blade, explorer, mourner, plenty sherds |
| warm ocean ruins | angler, shelter, snort sherds |
| desert well | arms_up, brewer sherds (`desert_well` is not a locatable *structure* in the registry, so the task explores desert biome and lets the block scanner find the well) |

`armadillo_scute` is the same mechanic pointed at an armadillo (brush the mob until it sheds).

### Mob buckets — `FillBucketWithEntityTask`

Cod, salmon, pufferfish and tropical fish, tadpole and axolotl need a **water bucket** used on the
mob; the sulfur cube needs an **empty bucket** and must not be a baby; powder snow is filled by
right-clicking the block with an empty bucket. The task finds a reachable mob, equips the right
container, aims, right-clicks the entity and verifies the bucket item appeared.

### Villager trades — `TradeWithVillagerTask`

The cartographer maps are trade-only. The task locates a village, finds (or, failing that, makes) a
cartographer, opens the merchant screen, and buys the wanted offer, earning emeralds and levelling the
villager up as needed (cartographer level 2 sells the village maps, 3 the ocean-monument/trial-chamber
maps, 5 the woodland-mansion map and the globe banner pattern). `TradeChains` routes the 16 structure
maps, the `globe_banner_pattern`, and the fish buckets that have trades. The Nether/End-only items
are documented gaps below.

## Remaining gaps

Every one of these was traced to the mechanic that produces it; none is a missing catalogue line.

| Category | Items | What it actually needs |
|---|---|---|
| Skeleton-killed-creeper discs | `music_disc_11`, `blocks`, `chirp`, `far`, `mall`, `mellohi`, `stal`, `strad`, `wait`, `ward` | not a drop table: a creeper must be killed by a skeleton's arrow. A scripted trap, not a loot source |
| Husbandry / time mechanics | `blue_egg`, `brown_egg`, `pitcher_pod`, `turtle_scute` | cold/warm chicken *laying* (random minutes-long wait), sniffer *digging*, baby turtle *growing up* (20 minutes). A bot can model "wait"; it is not a chain, it is a farm |
| End game | `elytra`, `dragon_breath` | end-ship navigation + item-frame looting, and bottling a dragon-breath cloud during the dragon fight |

## Known limitations of this audit and these chains

- **"Referenced" is not "obtainable."** An item that appears anywhere in the source still counts as
  referenced, so items merely *named* by a task are counted optimistically here; the buckets below it
  (craft/mine/chain/loot/none) are computed from game data and from the coverage registry.
- **The structure-loot chains need a singleplayer world.** The integrated-server locator has no
  equivalent on a multiplayer server, so on a server those items fall back to "no source known".
- **Random loot is random.** The trial-chamber vault tables and archaeology tables roll; the tasks cap
  their site/pot/vault attempts (8 structures / 120 blocks of brushing / 12 minutes of bucket attempts)
  and then stop with a warning rather than grind forever.
- **Underwater, deep-cave and desert-well sites are best-effort.** Ocean-ruin brushing and the sulfur
  cube (which spawns in sulfur caves) depend on the pathfinder getting there; a missed site is a
  give-up warning, not a silent claim.
- **`heavy_core` is a block item** (so the generic mine chain exists on paper) but it only generates as
  ominous-vault loot; the chain reaches it through the ominous-vault search rather than by mining.
- The audit's "mineable block" approximation matches item ids against the field names of
  `net.minecraft.world.level.block.Blocks`; two renamed pairs are mapped by hand
  (`dry_short_grass`→`short_dry_grass`, `dry_tall_grass`→`tall_dry_grass`), and the real test at
  runtime is `Block.byItem(item) != AIR`, which the code performs.

## How the measurement works

| Set | Source |
|---|---|
| The item registry | the field names of `net.minecraft.world.item.Items` (via `javap`) |
| Curated / referenced / coverage | every `Items.CONSTANT` in the mod's Java source, plus every item id in `adris/altoclef/chains/coverage/*.java` |
| Craftable | `data/minecraft/recipe/*.json` → result ids |
| Mob drops | `data/minecraft/loot_table/entities/*.json` |
| Loot / trade | `data/minecraft/loot_table/**` and `data/minecraft/villager_trade/*.json` |

Each registry item is then classified **in the order the chain resolver actually tries them**:
curated/chain → craftable → mineable block → mob drop → loot/trade → nothing.

## Extending coverage

1. **Prefer data over guesses.** Enumerate the jar (`loot_table`, `recipe`, `villager_trade`) and find
   the items with no chain; that is how every chain above was found.
2. **Add a curated entry** in `TaskCatalogue` for anything the generic chains cannot reach — `mob(...)`
   for drops, `mine(...)` for harvests, `smelt(...)`/`smith(...)` for conversions.
3. **Add a coverage chain** for a whole new *source family* (a new task, registered through
   `CoverageRegistry`); the structure-loot, archaeology and trade families are the worked examples.
4. **Watch the entity class.** `EntityTracker` keys by *exact runtime class* (`squashType` only folds
   Players), so an elder guardian is not a guardian, and `zombie_nautilus` is not a nautilus.
5. **Only add a chain you would believe.** A `mob(...)` entry for an item a mob *sheds* is a chain
   that can never complete; a documented gap beats a lying chain.

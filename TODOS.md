# TODOS

Forward-looking work for the UnionClef 26.3 native port. Newest thinking at the top of each section;
nothing here is a bug report, and everything here is *not done*.

> **Note on the old TODOS.md.** Code comments across this tree reference `TODOS.md` by ID — `C7.3`
> (MCP server binds `0.0.0.0` with only a bearer token and wildcard CORS), `C7.5`
> (`searchTimeoutMs` mutation), `C7.6` (butler config), `MCP-MISSING-WORLD-READ-TOOLS-2026-09-04`,
> and a 2026-08-24 drowning-death entry. That file was removed with the fork's stale docs during the
> documentation rewrite, so those references now dangle. The IDs below are listed so a reader knows
> what is missing; the original text is not recoverable from this repository.

---

## 1. Release mechanics

**1.0.0 was published on 2026-10-23. 0.97.0 was published on 2026-10-16** —
<https://github.com/smallduuude/unionclef-26.3-native/releases/tag/v0.97.0>

- [x] ~~`git init` and publish~~ — done. `main` is live, local and remote in sync, and the repo has a
      description, topics, wiki/projects off, issues on, and delete-branch-on-merge.
- [x] ~~Tag and release~~ — done. Tag `v0.97.0` pushed, the 11.26 MB jar attached, and
      `docs/releases/0.97.0.md` is the release body.
- [x] **1.0.0 published on 2026-10-23.** `main` tag `v1.0.0` and branch `26.2` tag
      `v1.0.0-26.2` are pushed, the release bodies are `docs/releases/1.0.0.md` / `1.0.0-26.2.md`,
      and both jars are attached. **VirusTotal scans are done and pinned:** 0 detections (0/63); the report links sit
      next to the SHA-256s in the READMEs and release notes.
- [ ] **Per release: re-scan the jar on VirusTotal, and update the hash.** The README and the release
      body both carry a scan link **pinned to one file's SHA-256**. A new jar has a new hash, so a new
      version needs a new scan and the new digest in *both* places — otherwise the link points at a
      file nobody is downloading, which is worse than having no link at all. This is now a required
      step in `docs/RELEASE.md`. **1.0.0 is an exception: it was published without a scan** (no API key was available,
      and the release was pushed during a water-damage emergency). The hashes in the README and
      release notes match the attached assets; the VirusTotal links still need filling in.
- [ ] *(optional)* `CONTRIBUTING.md`. Not required for a release.

## 2. Verification — the biggest gap by far

**Nothing in this port has been measured against a control.** Every improvement is reasoning from the
code and from 26.3's own data. That is better than guessing, but it is not evidence, and this
codebase's own comments are measurement-first for exactly that reason.

- [ ] **Operator playtest of 1.0.0 (both versions).** The automated runs already covered the parts
      that used to be guesses: bread crafted at a placed table (three in about five seconds, via the
      manual fallback), the crosshair-gated combat (off-crosshair swings refused, only on-target
      swings sent), the structure locator on both worlds, the ominous trial key/vault chain on 26.3
      and a clean shutdown on both. A human should still judge the feel: the crafting window on a
      large deposit, combat (which is deliberately slower now), the GUI and deposit flows, and one
      `@get` from each coverage family.
- [ ] **One playtest with the counters read out.** The instruments exist for every change made:
      `navReplanGaveUp`, `dbClearDeferred`, `kaAuraNotFacing`, `baritonePickups`, `baritoneMines`,
      `settingsApplied`, `pdBaritone`, `navSearchOnly`, `stallWhy*`. Reading them over a normal
      session would turn several guesses into facts — and would probably reveal which "improvement"
      is actually a regression.
- [ ] **A bare-engine comparison.** Baritone vs tungsten on the same route, same seed, same goal, so
      the engine choice is a measurement rather than an argument.
- [ ] **A frame-rate / memory pass.** The test machine is 8 GB and the launcher already warns. The
      shutdown fix (see `docs/releases/0.97.0.md`) removed a process that lingered after every quit;
      nothing has been done about steady-state cost.

## 3. Item coverage — what `@get` still cannot reach

Full data in [`docs/features/ITEM_BLOCK_COVERAGE.md`](docs/features/ITEM_BLOCK_COVERAGE.md), which has
the item → loot-table map, the coverage-chain inventory and the remaining-gap reasoning. Measured
state (2026-10-22): **1127 of 1143 survival-relevant items chain-covered on 26.3** (98.6%), 1082 of
1098 (98.5%) on 26.2; 16 gaps per version.

Done in the 2026-10-22 coverage pass (kept here so the list does not get re-litigated):

- [x] ~~**Structure loot.**~~ Cheat-free singleplayer locator (`StructureLocator`: integrated-server
      `ChunkGenerator.findNearestMapStructure` with a structure `HolderSet`, on the server thread) plus
      `LootSearchTask` (locate → travel → loot containers/pots/non-ominous trial vaults, retry another
      structure up to 8 times). Routes the nautilus armour, echo shard, trial key, banner patterns,
      goat horn, trident, horse armours, dungeon discs, bastion/Nether pair, and the 26.3 abandoned-camp
      maps. Limitation: singleplayer only — there is no integrated server to ask on a multiplayer
      server, and `/locate` needs op/cheats, which this mod does not use.
- [x] ~~**`@locate_structure` for any structure.**~~ `@locate_structure <id> [go]` (singleplayer).
- [x] ~~**Archaeology (20 sherds + `music_disc_relic`).**~~ `BrushBlockTask`; also `armadillo_scute`.
- [x] ~~**Mob buckets (8).**~~ `FillBucketWithEntityTask` + powder-snow bucket.
- [x] ~~**Cartographer maps (16) + `globe_banner_pattern`.**~~ `TradeWithVillagerTask`; a real levelling
      trade grind, and wandering-trader-only items are deliberately not routed.
- [x] ~~**Ominous trial-chamber family (4).**~~ `OminousKeyTask` (ominous bottle → trial spawner →
      ominous wave → ejected key) plus the ominous-vault mode of `LootSearchTask`; this is what
      unblocks the mace (`heavy_core`), `flow_banner_pattern`, `music_disc_creator` and
      `ominous_trial_key`.

Remaining gaps (each needs a mechanic, not a catalogue line; 16 items per version):

- [ ] **Skeleton-killed-creeper discs — 10 items** (`music_disc_11`, `blocks`, `chirp`, `far`, `mall`,
      `mellohi`, `stal`, `strad`, `wait`, `ward`). Not a drop table; needs a scripted trap that gets a
      skeleton's arrow to kill a creeper.
- [ ] **Husbandry/time mechanics — 4 items.** `blue_egg`/`brown_egg` (cold/warm chickens *lay* on a
      minutes-long random timer), `pitcher_pod` (sniffer *digs*), `turtle_scute` (baby turtle *grows
      up*). A bot can model "wait"; it is a farm, not a chain. `armadillo_scute` is done (brushing).
- [ ] **End game — 2 items.** `elytra` (End Ship item frame: end-city navigation + item-frame looting)
      and `dragon_breath` (bottle a dragon's breath cloud during the fight; the mod only *avoids* the
      clouds today).
- [ ] **Creative-only items are not targets** (`*_spawn_egg`, `debug_stick`, `knowledge_book`,
      `command_block_minecart`, and the non-survival set listed in the coverage doc) — no survival
      source exists in vanilla.

## 4. Chains

- [ ] **No source fallback.** `TaskCatalogue.getItemTask` picks *one* source in order
      (curated → craft → mine → smelt). If mining finds nothing the chain dead-ends instead of trying
      the next source. A composite task that tries them in order is the honest fix.
- [ ] **Chain arbitration is untouched.** All the chains work was in the *catalogue*. The
      `getPriority()` values that decide which chain wins when several apply (food vs defence vs
      gather) have never been reviewed, and they are the most user-visible part of "what the bot
      decides to do".
- [ ] **Generic smelting deliberately skipped** — the curated set already covers every meaningful
      smeltable (`FurnaceRecipeDisplay` is available if that changes).

## 5. Baritone surface

- [ ] **~15 of ~200 settings are set.** The chosen set is justified in
      `BaritoneBridge.applyOptimalSettings`; the planner's own tuned values (`costHeuristic`,
      `maxCostIncrease`, `backtrackCostFavoringCoefficient`, the tool settings) were deliberately left
      alone. Worth re-examining only with measurement, not intuition.
- [ ] *(optional)* **The full settings GUI.** The **Pathfinder...** screen offers 8 knobs with their
      live values; the request was for Baritone's settings generally. Operator has said the current
      defaults are good, so this is polish.

## 6. Known bugs

- [ ] **`TimeoutWanderTask.onTick:366` threw twice** (`crash-2026-10-14_12.34.38` and
      `_17.40.12`, "Unexpected error"). It has not recurred since 10-14, and nothing has touched that
      line. It is the only unexplained in-game crash in the logs.
- [ ] **Baritone mixin conflict**: `tungsten.mixins.json:MixinClientPlayerEntity` (`allowManualFlight`)
      is **skipped** because `mixins.baritone.json` redirects the same method first. Harmless as far as
      we know — manual flight during pathing — but it means tungsten's version of that hook is dead
      whenever Baritone is installed.

## 7. Decided against (recorded so it does not get re-litigated)

- ~~Command merges: `timeout`/`t`, `coverwithsand`/`coverwithblocks`.~~ Operator: not needed.
- ~~A settings GUI for all ~200 Baritone values.~~ Operator: current defaults are good.
- ~~Porting combat/aim out of tungsten.~~ Baritone has no combat, and operator reports combat works
      well as-is. Only revisit if tungsten is ever dropped entirely — that is when it becomes load
      bearing, not before.

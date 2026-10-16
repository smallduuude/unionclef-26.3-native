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

**0.97.0 was published on 2026-10-16** —
<https://github.com/smallduuude/unionclef-26.3-native/releases/tag/v0.97.0>

- [x] ~~`git init` and publish~~ — done. `main` is live, local and remote in sync, and the repo has a
      description, topics, wiki/projects off, issues on, and delete-branch-on-merge.
- [x] ~~Tag and release~~ — done. Tag `v0.97.0` pushed, the 11.26 MB jar attached, and
      `docs/releases/0.97.0.md` is the release body.
- [ ] **Per release: re-scan the jar on VirusTotal, and update the hash.** The README and the release
      body both carry a scan link **pinned to one file's SHA-256**. A new jar has a new hash, so a new
      version needs a new scan and the new digest in *both* places — otherwise the link points at a
      file nobody is downloading, which is worse than having no link at all. This is now a required
      step in `docs/RELEASE.md`.
- [ ] *(optional)* `CONTRIBUTING.md`. Not required for a release.

## 2. Verification — the biggest gap by far

**Nothing in this port has been measured against a control.** Every improvement is reasoning from the
code and from 26.3's own data. That is better than guessing, but it is not evidence, and this
codebase's own comments are measurement-first for exactly that reason.

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
the item → loot-table map and the reasoning. Summary of what is missing and what each needs:

- [ ] **`LootSearchTask` — 28 items (biggest bucket).** Locate a structure by its loot-table name,
      travel, and loot the chests for the wanted item.
      **BLOCKED ON A DECISION:** there is no generic structure locator in this codebase —
      `@locate_structure` hand-rolls stronghold and desert temple only, and vanilla
      `/locate structure` needs cheats/op. Choose first:
      explore-and-recognise (block signature per structure) vs server `/locate` when permitted, vs
      both with a fallback.
- [ ] **`BrushBlockTask` — 21 archaeology items.** Find `SUSPICIOUS_SAND`/`SUSPICIOUS_GRAVEL`, travel,
      hold a brush and hold use. Note the drop is *structure-dependent*, so this can only be "brush
      until you get the wanted sherd".
- [ ] **Bucket-a-fish — 3 items.** `cod`/`pufferfish`/`tropical_fish`_bucket: use a bucket on the
      mob. Small variant of the existing liquid-bucket collection.
- [ ] **Husbandry — 5 items.** `blue_egg`/`brown_egg` (chickens *lay*), `armadillo_scute` (armadillos
      *shed*), `turtle_scute` (baby turtles *grow into*), `pitcher_pod` (sniffers *dig up*). Deliberately
      last: a bot waiting for an egg is a poor use of it, and a `mob(...)` entry for a shed item is a
      chain that can never complete.
- [ ] **Maps — ~6 items.** Cartographer trades. `TradeWithPiglinsTask` is the only trade machinery
      that exists; a villager-trade chain would need building.

### Separate projects (not catalogue entries)

- [ ] **`elytra`** — End Ship item frame. Needs end-city navigation plus item-frame looting.
- [ ] **`dragon_breath`** — bottle a dragon's breath cloud. The mod only *avoids* the clouds today
      (`DragonBreathTracker`).
- [ ] **Pre-26.3 music discs** (`11`, `13`, `blocks`, `cat`, `chirp`, `far`, `mall`, `mellohi`,
      `stal`, `strad`, `wait`, `ward`, `pigstep`) — a **creeper killed by a skeleton's arrow**. A
      mechanic, not a drop table; no task models it.
- [ ] **Creative-only items are not targets** (`*_spawn_egg`, `debug_stick`, `knowledge_book`,
      `command_block_minecart`) — no survival source exists in vanilla.

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

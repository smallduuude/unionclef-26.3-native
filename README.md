# UnionClef

## ⚠️ ALPHA BUILD — expect rough edges

> **UnionClef is in alpha.** Things will break, behaviour will change between versions, and some
> features are half-finished on purpose while they are being reworked. Use it accordingly.
>
> If you find **anything** that is buggy, janky, or just mildly annoying — **no matter how small** —
> **please post it to Issues.** Short reports are welcome; you do not need a perfect reproduction.
>
> **➡️ [Open a bug report or a feature request](../../issues/new/choose)**

**A chat-driven automation bot for Minecraft 26.2 (Fabric).** Type `@help` in chat and a
computer plays the game for you — mining, crafting, fighting, exploring, and beating the game.

UnionClef is the union of two things:

- **altoclef** — the task/command brain. You talk to it in chat; it decomposes what you asked
  for into a tree of tasks and executes them.
- **Baritone** — **bundled and required.** The separate
  **[Baritone](https://github.com/cabaletta/baritone)** mod (LGPL-3.0) drives every task's movement:
  pathing, mining and collecting drops. It ships inside this jar as a Fabric nested JAR, so there is
  nothing to install — and if you drop your own Baritone in `mods/`, Fabric uses **yours**.
- **tungsten** — bundled. Owns **aiming, bow shooting and combat**, which Baritone has no equivalent
  for. Its pathfinder stays as a runtime safety net if the Baritone bridge ever fails to initialise.

See [Choosing the pathfinder](#choosing-the-pathfinder).

Built for **Minecraft 26.2**, **Java 25**, **Fabric**.

---

## Features

- **Play the game from chat.** `@get diamond 3`, `@goto 100 64 -200`, `@hero`, `@gamer`.
- **Full task tree.** Resources, crafting (table/anvil/smithing), smelting, containers/stashes,
  combat, shelter, nether portal construction, and an end-to-end `@gamer` (beat-the-game) run.
- **Humanised movement.** Baritone paths, mines and collects dropped items; tungsten's human-like
  mouse pipeline aims (WindMouse rotation), and the engine is configured for anti-cheat-safe movement
  out of the box.
- **Registry-wide item sourcing.** `@get` resolves **any** item in the 26.2 registry, not a curated
  shortlist: it crafts from the server's own recipe book, mines the block, smelts, or hunts the mob
  that drops it. 26.2's new content is covered — sulfur, resin, pale oak, the copper family,
  the mace, nautilus shells and the nautilus-armour family's ingredient.
- **Mining and collecting belong to the engine.** altoclef decides *what* and *when*; Baritone
  decides *how* — including opening the doors on the way to a drop.
- **Defence built in.** Auto food, mob defence, MLG water-bucket saves, projectile dodging, and
  auto resource-pack accept so a headless bot is not kicked.
- **In-game settings screen** (Cloth Config) for tweaking behaviour without editing files.
- **Baritone does the moving.** Baritone is required and handles pathing, mining and collecting; the
  bundled engine stays selectable in the **Pathfinder...** screen for comparison, and owns aim and
  combat either way.
- **Automation bridges.** An opt-in **Py4j** gateway and a **LAN MCP server** let external
  programs (including LLM agents) drive the bot on the same levers as chat commands.
- **Optional voice-chat integration** when Simple Voice Chat is present.

---

## Requirements

| Component | Version |
|---|---|
| Minecraft | **26.2** (declared `~26.2`) |
| Fabric Loader | `>= 0.19.0` |
| Fabric API | `0.161.0+26.2` |
| Baritone | **bundled** — nothing to install (v1.19.0 ships inside the jar; your own copy in `mods/` takes priority) |
| Cloth Config | `>= 26.2` *(bundled — do not install separately)* |
| Java | **25** |

The release jar bundles Cloth Config, Jackson, the Nether pathfinder, Py4j, tungsten **and Baritone** —
one file, nothing else to install.

---

## Installation

1. Install **Fabric Loader 0.19.0+** for **Minecraft 26.2** ([fabricmc.net](https://fabricmc.net/use/installer/)).
2. Put **[Fabric API](https://modrinth.com/mod/fabric-api)** in your `mods/` folder.
3. Put **`unionclef-<version>+26.2.jar`** in the same `mods/` folder.
4. Launch with **Java 25**.

**That's it — one file.** Baritone is a required dependency, and it **ships inside the UnionClef jar**
as a Fabric nested JAR, so there is nothing else to download. If you already have your own Baritone in
`mods/`, Fabric Loader prefers it and ignores the bundled copy — updating Baritone yourself is
supported, not something to avoid.

> Cloth Config, Jackson, the Nether pathfinder, Py4j, tungsten **and Baritone** are all inside the
> UnionClef jar. Installing any of them separately will not break anything, but it is redundant.

---

## Antivirus — and verifying your download

A mod that automates a player and writes inside your game directory gets flagged by heuristic
scanners from time to time. **This 26.2 build has not been uploaded to VirusTotal yet**; the 26.3
build of the same version was clean. The digest is the part that matters, and GitHub publishes it on
the release asset:

```
SHA-256  293aca05add26188383e0874f193607ca0b0fa63c82c56921a4a9d3b78aeb697
```

Check your download against it:

```powershell
Get-FileHash unionclef-0.97.0+26.2.jar -Algorithm SHA256     # Windows
```
```bash
shasum -a 256 unionclef-0.97.0+26.2.jar                      # macOS / Linux
```

If it matches, you have the exact file this README describes.

> **What this is and is not.** A scan is *evidence, not a guarantee* — and it is only valid for the
> exact file it was run on. **The VirusTotal report linked from the 26.3 release covers that file,
> not this one**; if you want a report for this build, upload it yourself and compare the digest.
> For the curious: the mod phones nothing home, downloads nothing, and writes only under
> `<game dir>/altoclef/`. The two things scanners dislike about game mods are reading process memory
> and writing files, which is exactly what a pathfinding bot has to do. The optional **Py4j** and
> **MCP** bridges *do* open local network ports — they are **off by default** and must be enabled
> deliberately (see [Automation bridges](#automation-bridges-advanced-optional)).

---

## Usage

### Quick tasks — no commands needed

Press the **UnionClef** keybind (default **K**, rebindable in *Options → Controls → Miscellaneous*)
to open the hub screen.

**Get any item:** type part of an item's name in the **Get an item** box and a live suggestion row
shows matching items by their natural name (type `dia` → *Diamond*, *Diamond Block*, …). Pick one,
set the amount, press **Get** — the bot gathers it for you. This reaches the whole item catalogue,
not just a short list.

**One-click tasks:** *Kill Hostiles*, *Self-Care*, *Get Food*, *Beat the Game*, *Go to Nether*,
*Deposit Items*, *Pause*, *Stop*, plus **Help** (lists every command).

Everything is greyed out until you are in a world. The same screen also offers the **Settings...**
button (full options screen) and a **command box** at the bottom for anything not on a button.

### Choosing the pathfinder

Press the hub key (**K**) and click **Pathfinder...**. The screen has three parts:

- **Engine** — **Baritone** is required and is what drives the bot. **Tungsten (buggy)** is this
  mod's own port of Baritone's planner, bundled, and kept selectable for comparison or if the
  Baritone bridge ever fails to initialise.
- **Go to** — type X/Y/Z and press **Go**. Tungsten is sent `@goto`; Baritone is sent its own
  `#goto`. X and Z are required; leave Y blank to keep the bot's height.
- **Baritone settings** — type a name and a value and press **Apply**; each row is sent as
  `#set <name> <value>`, and the fields start from the value actually in force.

Whichever engine is driving, the **normal UnionClef commands** (`@get`, `@goto`, `@hero`, `@food`,
`@gamer`, `@follow`, …) move the bot with it, and `@stop` stops it. Baritone ships **inside this jar**,
so there is nothing to install; if you already have your own copy in `mods/`, Fabric Loader prefers it
— updating Baritone yourself is supported, not something to avoid.

### Chat commands

Everything is prefixed with `@` and typed into the in-game chat:

```
@help                       # list every command
@get diamond 3              # collect 3 diamonds (mines, crafts, smelts as needed)
@goto 100 64 -200           # travel to coordinates
@goto NETHER                # travel to a dimension (names are UPPERCASE)
@hero                       # kill nearby hostiles
@deposit                    # bank everything non-equipped in a nearby container
@gamer                      # beat the game
@stop                       # stop whatever it is doing
```

`@help` prints the authoritative, always-current list. A selection of common commands:

| Command | What it does |
|---|---|
| `@gamer` | Beat the game (gather gear, nether, stronghold, ender dragon). |
| `@get <item> [count]` | Obtain an item or resource, crafting/smelting as required. |
| `@give <item> [player]` | Collect an item and hand it to you or another player. |
| `@goto <x y z / x z / y / dimension>` | Travel to coordinates or a dimension. |
| `@hero` | Kill all nearby hostile mobs. |
| `@food` / `@meat <count>` | Gather food. |
| `@selfcare` | Gear up and look after itself. |
| `@deposit` | Deposit *all* carried items into a nearby container. |
| `@stash <x y z x y z>` | Store items in a container stash at a region (blank item list = all non-equipped). |
| `@follow <player>` | Follow a player. |
| `@mirror <player>` | Record and replay a player's movement path. |
| `@equip <item>` | Equip an item. |
| `@inventory [item]` | Print the inventory, or count a specific item. |
| `@list` | List all obtainable items. |
| `@locate_structure <structure>` | Locate a world-generated structure. |
| `@coords` | Print the bot's current coordinates. |
| `@status` | Report the running command's status. |
| `@pause` / `@idle` / `@wander` | Pause, stand still, or idle-life so it does not look frozen. |
| `@set <setting> <value>` / `@set list` | Change a setting at runtime. |
| `@reload_settings` | Reload settings and the butler lists from disk. |
| `@timeout <sec> <command>` / `@t <command>` | Run a command with a timeout (default 60 s). |
| `@agent <string>` | Run an agentic command through the Python entrypoint. |
| `@stop` | Stop the current task. |

**Three things worth knowing:**

- **`@get` gathers on top of what you hold.** `@get bed 1` with a bed already in your pack fetches
  *one more* — it does not consider the job done because you have one. (Internal tasks used by
  `@gamer` and the rest still mean "ensure I have at least N"; only the command works this way.)
- **`@get` resolves any real 26.2 item id**, so a name the curated catalogue has never heard of still
  works. See [What it can't reach yet](#what-it-cant-reach-yet) for the exceptions.
- **`@help` lists what a player needs, not everything.** A few debug and agent commands (`@test`,
  `@dummy`, `@agent`, `@custom`, `@gamma`, `@scan`, `@nick`, `@pearl`, …) stay callable by name —
  scripts and the agent surface use them — but are deliberately left out of the list.

### Settings

- **In game:** press the **UnionClef Settings** keybind — *Options → Controls → Miscellaneous*,
  default **K** — to open the Cloth Config screen and change options live.
- **On disk:** configuration is JSON under `<game dir>/altoclef/` — most importantly
  `altoclef_settings.json` (created on first launch). Reload after editing with `@reload_settings`.

### Automation bridges (advanced, optional)

Both bridges are **disabled unless you opt in** and share one set of levers with chat commands.

| Bridge | Default | Purpose |
|---|---|---|
| **Py4j gateway** | `127.0.0.1:25333` | Python programs call into the bot (`Py4jEntryPoint`). |
| **MCP server** | `0.0.0.0:25350` | Streamable-HTTP MCP endpoint (`POST /mcp`) for LLM agents. |

The MCP server mints a random bearer token on first start, stored as `mcpAuthToken` in
`altoclef_settings.json`; every request needs `Authorization: Bearer <token>` or it is rejected.

```bash
claude mcp add --transport http unionclef http://<bot-lan-ip>:25350/mcp \
  --header "Authorization: Bearer <mcpAuthToken>"
```

See [`docs/features/MCP_SERVER.md`](docs/features/MCP_SERVER.md) and
[`docs/features/AGENT_PY4J_LEVERS.md`](docs/features/AGENT_PY4J_LEVERS.md) for the full lever set.

---

## Building from source

Requires **JDK 25** (the build targets Java 25 and the game runs on it).

```bash
git clone https://github.com/smallduuude/unionclef-26.3-native
cd unionclef-26.3-native
./gradlew build            # Windows: gradlew.bat build
```

The distributable, remapped mod jar is written to:

```
build/libs/unionclef-<mod_version>+26.2.jar
```

Other useful tasks:

```bash
./gradlew compileJava      # compile only (altoclef + tungsten)
./gradlew runClient        # build and launch a dev Minecraft client
./gradlew clean build      # reproducible full build
```

The first build downloads the Gradle distribution plus Minecraft/Fabric (~1 GB) and caches it.
No pre-built jars, no `publishToMavenLocal`, no submodules — `tungsten` is a sibling Gradle
project (`:tungsten`) resolved and bundled automatically.

### What's inside the jar

UnionClef classes, tungsten, the Nether pathfinder, Jackson (core/annotations/databind),
Py4j, and Cloth Config. This is a standard Fabric jar-in-jar bundle — users install one file.

---

## Project layout

```
build.gradle            Root build (altoclef mod, bundles :tungsten)
gradle.properties       mod_version / minecraft_version / dependency versions
settings.gradle.kts     Root + :tungsten
src/main/java/          altoclef — bot, tasks, commands, trackers, UI, Py4j + MCP bridges
src/main/resources/     fabric.mod.json, mixins, assets
tungsten/               tungsten — fallback pathfinder, plus aim and combat (bundled subproject)
TODOS.md                outstanding work and the things decided against
docs/                   Developer and reference documentation
  releases/             Per-version release notes
```

---

## Documentation

Full docs live in [`docs/`](docs/):

- [`docs/DEVELOP.md`](docs/DEVELOP.md) — build the project, run a dev client, troubleshooting
- [`docs/RELEASE.md`](docs/RELEASE.md) — versioning and how a release is published
- [`docs/features/`](docs/features/) — the MCP control surface, the agent lever catalog, and the
  [item/block coverage audit](docs/features/ITEM_BLOCK_COVERAGE.md)
- [`docs/releases/`](docs/releases/) — release notes, newest first
- [`docs/README.md`](docs/README.md) — the docs index
- [`TODOS.md`](TODOS.md) — outstanding work, and the things decided against
- [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) — bundled and runtime dependencies and licences

---

## What it can't reach yet

`@get` covers the large majority of the 26.2 registry, and the gaps are **known** rather than
unknown. The full audit — every item, its source, and how it was measured — is in
[`docs/features/ITEM_BLOCK_COVERAGE.md`](docs/features/ITEM_BLOCK_COVERAGE.md). It was measured
against the 26.3 client jar; this build ships the same chains over the 26.2 registry, which lacks
the handful of items introduced in 26.3 (for example `poplar`).

- **Structure loot** — 68 items, including `echo_shard` and `disc_fragment_5` (ancient city), trial
  keys, the nautilus-armour set, and most explorer maps and banner patterns. Needs a "locate the
  structure, travel, loot the chests" task, and that is blocked on deciding how a bot finds a
  structure at all — vanilla `/locate structure` requires cheats or op.
- **Archaeology** — 21 pottery sherds and armour trims. Needs a brushing task.
- **Husbandry** — `blue_egg`, `brown_egg`, `armadillo_scute`, `turtle_scute`, `pitcher_pod`. These are
  *laid*, *shed*, *grown into* or *dug up*, not dropped on death, so there is deliberately no
  kill-and-loot chain for them: a chain that cannot complete is worse than a documented gap.
- **Separate projects** — `elytra`, `dragon_breath`, and the older **music discs**, which come from
  a creeper killed *by a skeleton's arrow* (a mechanic, not a drop table).
- **Creative-only items** (`*_spawn_egg`, `debug_stick`, `knowledge_book`, `command_block_minecart`)
  have no survival source in vanilla and are not targets.

Everything outstanding, in the order it should be tackled, is in [`TODOS.md`](TODOS.md).

---

## Status

UnionClef for Minecraft 26.2 is an **alpha**. The port to official Mojang mappings on Java 25 is done;
the movement engine is **Baritone end to end** — pathing, mining and collecting — with tungsten kept
for aim and combat, and the in-game GUI drives it without any command syntax.

**It has not been playtested by its maintainer.** Every fix in the current build came from a user
report, and the project's own counters — the instruments that would turn its tuning from argument
into measurement — are listed in [`TODOS.md`](TODOS.md) as the highest-priority outstanding work.
Treat it accordingly.

Bug reports are welcome — attach a log and the exact `@command` (or GUI action) that reproduces the
issue. There is no minimum size to a report; small annoyances are exactly what the issue tracker is
for.

---

## Credits & License

Original UnionClef by **3ndetz** — <https://github.com/3ndetz/unionclef>.
UnionClef is the union of **AltoClef** and **Tungsten**; contributors include TacoTechnica,
James Green, Marvion Kirito, MiranCZ, and 3ndetz. The 26.2 native port is maintained by
**smallduuude** — <https://github.com/smallduuude/unionclef-26.3-native>.

### Third-party software

| Component | Licence | Bundled? |
|---|---|---|
| [Baritone](https://github.com/cabaletta/baritone) | **LGPL-3.0** | **Yes** — a Fabric nested JAR (`META-INF/jars/`), unmodified; its licence ships at `META-INF/licenses/LGPL-3.0.txt`. A copy in `mods/` takes priority. |
| Tungsten | GPL-3.0 (part of this project) | yes, as a subproject |
| [Nether Pathfinder](https://github.com/babbaj/nether-pathfinder) | MIT | yes |
| [Jackson](https://github.com/FasterXML/jackson) | Apache-2.0 | yes |
| [Py4j](https://www.py4j.org/) | BSD-3-Clause | yes |
| [Cloth Config](https://github.com/shedaniel/cloth-config) | LGPL-3.0 | yes |
| [Simple Voice Chat API](https://github.com/henkelmax/simple-voice-chat) | MIT (compile-time only) | no |

Baritone **is** redistributed here — unmodified, as a Fabric nested JAR — because it is a required
dependency and a user should not have to go and find it. Its LGPL-3.0 licence travels with it at
`META-INF/licenses/LGPL-3.0.txt`. You can replace it with your own copy in `mods/`: Fabric Loader
resolves one version per mod id and prefers yours.

Copyright © the UnionClef contributors. Released under the **GNU General Public License v3.0**
(GPL-3.0-only) — see [`LICENSE`](LICENSE) for the full text. As required by the GPL, if you
distribute a modified UnionClef you must make the complete corresponding source available under
the same license.

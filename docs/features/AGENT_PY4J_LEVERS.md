# Agent levers (py4j / MCP)

Catalog of the **levers** an external agent uses to drive the bot, over either the py4j gateway
(port `25333`) or the mod's own MCP server (port `25350`; see [MCP_SERVER.md](MCP_SERVER.md)). The
mod provides primitives and data; **the agent decides** where, when and what.

Composition is **see → go → act**: `getGameState` (perception) → `gotoXYZ` / `pathStatus`
(movement) → combat / building / menus (action). The agent holds the strategy; the mod executes the
mechanics. Dictionary returns are Python dicts, and coordinates are world coordinates.

**Auth:** MCP requests need `Authorization: Bearer <token>`, generated on first start and stored as
`mcpAuthToken` in `altoclef_settings.json`. Start the py4j gateway on port `25333` (Settings).

## Perception — what is happening

| Lever | What it gives |
|---|---|
| `getGameState()` | `self` (hp / maxHp / armor / pos / onGround / held / blocks) + `players[]` (name / pos / distance / hp / sprinting, sorted by distance) + `beds[]` (within r=40). The main "eye" — read every decision cycle, no screenshots needed |
| `inGame()` | whether the client is in a world (bool) |
| `getHealth()` / `getHeldItem()` | HP / held item id |
| `nearestPlayersInfo(limit, asString)` / `getPlayersInfo(limit)` | the player list |
| `getCrosshairTarget()` | what the crosshair is looking at (block / entity) |
| `reachability(playerName)` | whether a player is reachable (reach / line of sight / angle) |
| `canReach(x,y,z,withBreaking)` | whether a cell is reachable (reached / pathSize / breaks / endDistance) |
| `canBreakBlock(x,y,z)` | whether breaking a block is allowed (deny list / zones / block entity) |
| `getBlockAt(x,y,z)` / `getGroundBlock()` | the block at a point / underfoot |
| `getOpenScreen()` | the open screen (title + all slots: id / name / count) |
| `getInventoryFull()` / `inventorySpace()` | inventory fullness / free slots + block counts |
| `getRecentChat(n)` | the last n chat messages |
| `getScreenshot()` | PNG bytes of the current frame |

## Movement — going somewhere

| Lever | What it does |
|---|---|
| `gotoXYZ(x,y,z)` | navigate to a coordinate through the **selected engine** — Baritone (required) by default, or the bundled fallback if you switch — walk / parkour / bridge. Fire-and-poll |
| `pathStatus()` | `busy` / `pos` / `distance` (to the `gotoXYZ` target) / `arrived`. `arrived` reads the engine's own arrival distance, so a poll loop cannot hang |
| `gotoFar(x,y,z,horizon)` | far target via a receding horizon: one segment up to `horizon` toward the target; poll `gotoFar → pathStatus → gotoFar` until `finalSegment` |
| `stopPathing()` | total stop (the engine's own stop + altoclef `@stop`) |
| `hasActiveTask()` | whether the bot is busy with a task |
| `bridgeForward(dir, n)` / `bridgeTo(x,y,z)` | continuous floor paving in a direction / to a target |
| `bridgeActive()` / `bridgePlaced()` | whether a bridge is running / how many blocks placed |

> **Movement goes through whichever engine is selected.** Baritone by default — pathing, mining *and*
> collecting dropped items, including opening the doors on the way. Baritone is a **required
> dependency** (Fabric will not launch UnionClef without it). Tungsten is bundled and still owns
> **aim, bow and combat**, which Baritone has no equivalent for; its pathfinder remains a selectable
> fallback if the Baritone bridge ever fails to initialise. Both `@goto` (altoclef command) and
> `gotoXYZ` (this lever) follow the selected engine.

## Aim and rotation (anti-cheat safe)

All rotations go through the mouse pipeline (like a physical mouse) — never `setYaw`/`setPitch`,
which anti-cheats flag.

| Lever | What it does |
|---|---|
| `lookAt(x,y,z)` / `lookAtPlayer(name)` | aim the crosshair at a point / a player |
| `rotateCamera(dYaw, dPitch)` | relative camera rotation |

## Combat — primitives, the agent holds the strategy

| Lever | What it does |
|---|---|
| `mouseClick("left"/"right"/"middle")` | a mouse click (hit / use) |
| `interactCrosshairEntity()` | right-click the entity under the crosshair |
| `interactEntity(name, use)` | attack / use a specific player |
| `attackPlayer(name)` / `isAttacking(name)` | set / check the attack target |
| `shieldBlock(ticks)` | raise the shield for N ticks |
| `solveArrowAim(name)` | bow ballistics with lead (yaw / pitch / charge), without firing |
| `shootArrowAt(name)` | bow shot with trajectory (aim → charge → track → release) |
| `useHeldItem()` | use the held item (food / pearl / potion) |
| `punk(name)` | hunt a player by name (tungsten: A* approach + aura) |
| `punkAny(allow, avoid)` | hit the nearest from `allow` (empty = any), leaving `avoid` alone; auto-retarget |
| `punkAvoid(avoid)` | update the avoid list on the fly |
| `punkStatus()` / `punkStop()` | current target + activity / stop combat |

## Building and WorldEdit (real placement, works in survival)

| Lever | What it does |
|---|---|
| `placeBlockAt(x,y,z)` | place a block in a cell (aim at the support face, interact) |
| `placeBlockLooking()` | place a block wherever the crosshair is looking |
| `select(x1,y1,z1,x2,y2,z2)` | set a WorldEdit region (highlighted) |
| `clearSelection()` | clear the selection |
| `fillSelection(block)` | `//set` — fill the region (bottom-up, within reach, capped per call; equips the block from the hotbar) |
| `wallsSelection(block)` | `//walls` — the four vertical walls of the region (hollow centre) |
| `buildDefenseAround(x,y,z)` | a protective shell around a point (sides + roof) |
| `canBreakBlock(x,y,z)` | whether breaking is allowed |
| `canPlaceBlock(x,y,z)` | whether placing is allowed (policy + replaceable) |
| `markProtectedArea(x,y,z,r)` | mark a private/claim — a cube of radius r the bot will not break or build in |
| `clearProtectedAreas()` | clear all runtime private areas |

`fillSelection` / `wallsSelection` return `remaining` > 0 when some cells are out of reach — move
closer with `gotoXYZ` and call again.

## Menus, shops, manual input

| Lever | What it does |
|---|---|
| `clickMenuByName(names, button, action, timeoutMs)` | click a menu slot **by item name** (shop / hub navigation) |
| `clickUiSlot(slot, button, action)` | click a menu slot by index |
| `selectHotbar(slot)` | select hotbar slot 0–8 |
| `getOpenScreen()` | read the open menu (see Perception) |
| `closeOpenScreen()` | close the screen |
| `screenClickAt(x,y,button,scaled)` | click at screen coordinates (any GUI) |
| `tapKey(name)` / `holdKey(name,ms)` | press / hold a key |

## Commands and communication

| Lever | What it does |
|---|---|
| `ExecuteCommand("@...")` | an altoclef command (`@goto`, `@get`, `@gamer`, `@stop`) |
| `ChatMessage(";...")` / `ChatMessage("text")` | a tungsten command (`;goto`, `;bridge`, `;stop`) or plain chat |
| `ConnectToServer(ip)` | connect to a server |

---

The single source of descriptions is the Javadoc on the `Py4jEntryPoint` methods; the MCP server
wraps them without duplicating logic. Update this list when you add a lever.

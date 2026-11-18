package kaptainwutax.tungsten.path.movements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import kaptainwutax.tungsten.TungstenModDataContainer;
import kaptainwutax.tungsten.util.WindMouseRotation;
import kaptainwutax.tungsten.world.BetterBlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Port of {@code baritone/src/main/java/baritone/pathing/movement/Movement.java} (1-296), the
 * abstract base of one one-block step: it prices itself once, breaks what is in the way before it
 * runs ({@link #prepared}), and is the SINGLE per-tick writer of keys and of the camera target
 * ({@link #update}). Verified line-by-line against {@code shredder/.../pathing/movement/Movement.java},
 * which is the same file already compiling on this MC version — that is where {@code getEntityPos()}
 * (vs upstream's {@code getPos()}) comes from.
 *
 * <p>Why the "release everything, then press exactly what this tick declared, then clear the map"
 * order is load-bearing (Movement.java:139-143): a key not set this tick is released this tick, so
 * nothing latches. That is what makes "one owner of the inputs" true rather than aspirational, and
 * it is the fix for measured pitfall P1 (BARITONE-PORT-SPEC.md: {@code called=11041 inRange=11040
 * clicked=0} — a second per-tick writer racing this one). Whatever ticks a Movement MUST therefore
 * suppress every other input/aim writer for that tick (BlockPathWalker, PathExecutor, combat).
 *
 * <h2>Adapters written here because tungsten has no baritone service</h2>
 * <ul>
 *   <li>{@code IPlayerContext} -&gt; {@link PlayerCtx}, a nested adapter with only the members the
 *       movements use. {@code playerFeet}/{@code playerHead} are ported verbatim
 *       (IPlayerContext.java:62-95) including the {@code +0.1251} and the slab correction;
 *       {@code objectMouseOver} is a LIVE raytrace, per BaritonePlayerContext.java:84-86 — never
 *       {@code mc.crosshairTarget}, which at the stand's measured 10 fps is one to two ticks stale,
 *       exactly the width of the sneak-pose and jump-apex windows.</li>
 *   <li>{@code LookBehavior.updateTarget(rot, force)} -&gt; {@link WindMouseRotation}: forced
 *       targets go to {@code setTarget}, unforced ({@code moveTowards}) to {@code setTargetFast}
 *       with the pitch preserved. The camera is NEVER moved with {@code setYaw}/{@code setPitch} —
 *       rotation has to go through the vanilla mouse pipeline or it is trivially detectable.</li>
 *   <li>{@code InputOverrideHandler} -&gt; {@link #clearAllKeys()} / {@link #applyInputs} over
 *       {@code MinecraftClient.getInstance().options}, including the
 *       CLICK_LEFT-cancels-CLICK_RIGHT interlock (InputOverrideHandler.java:94-96) and the
 *       {@code BlockPlaceHelper} 4-tick right-click gate (BlockPlaceHelper.java:29-57).</li>
 *   <li>{@code MovementHelper.switchToBestToolFor} -&gt; {@code TungstenModDataContainer.equipToolHook}
 *       (TungstenModDataContainer.java:30). Tungsten never touches the inventory itself; altoclef
 *       owns the hotbar, so baritone's {@code ToolSet} is not ported.</li>
 *   <li>{@code CalculationContext} -&gt; {@code (WorldView, ClientPlayerEntity)} per the spec's
 *       substitution table. {@code Baritone.settings()} -&gt; hardcoded upstream defaults, each
 *       named at its use site.</li>
 *   <li>{@code PathingBehavior.pathStart()} — no equivalent; see {@link #playerInValidPosition()}.</li>
 * </ul>
 *
 * <h2>What this file expects from the rest of the package (unit 1 contract)</h2>
 * {@code MovementState} ({@code setStatus}/{@code getStatus}/{@code setInput}/{@code getTarget}/
 * {@code getInputStates}, nested {@code MovementTarget(Rotation, boolean)} with {@code getRotation()}
 * and {@code hasToForceRotations()}), the {@code MovementStatus} enum (with {@code isComplete()}),
 * the {@code Input} enum, {@code Rotation} (with {@code getYaw}/{@code getPitch}/
 * {@code isReallyCloseTo}), {@code RotationHelper.reachable/calcRotationFromVec3d/getBlockPosCenter},
 * and {@code MovementHelperB.isLiquid/canWalkThrough/canWalkOn}.
 */
public abstract class Movement {

    /**
     * Movement.java:36. The order is load-bearing and is not to be sorted: DOWN last is what makes
     * {@code preferDown} work, and NORTH-first is what makes a side place deterministic.
     */
    public static final Direction[] HORIZONTALS_BUT_ALSO_DOWN_____SO_EVERY_DIRECTION_EXCEPT_UP = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN};

    /** Baritone {@code blockReachDistance}, Settings.java:385, and the creative 5.0 branch from
     *  IPlayerController.java:58-60. Hardcoded default, not a tungsten setting. */
    private static final double BLOCK_REACH_DISTANCE = 4.5;

    /** Baritone {@code pauseMiningForFallingBlocks}, Settings.java:370 = true. */
    private static final boolean PAUSE_MINING_FOR_FALLING_BLOCKS = true;

    /** Purely a telemetry threshold: how far the camera has to be from the requested facing before
     *  a tick counts as "steered" in {@link #motionSteered}. Nothing branches on it. */
    private static final float STEER_REPORT_DEG = 15.0f;

    // The 4-tick place cooldown moved to helpers/BlockPlaceHelper — see its header. It was
    // private to this class, so it governed the ported movements and nothing else; four other
    // placement sites (executor, bridge, pillar, the py4j build surface) had no rate at all.

    /**
     * TELEMETRY ONLY — no branch reads these. They exist because the split place engine's counters
     * ({@code PathExecutor.placeCalled/placeInRange/placeClicked}) are what proved it broken —
     * {@code called=11041 inRange=11040 clicked=0} — and the replacement has to be measurable at the
     * same granularity or "it works now" is a claim rather than a number. Exposed over py4j as
     * {@code placeStats}.
     *
     * <p>{@code placeRequested} = ticks a movement asked for CLICK_RIGHT (i.e. its own crosshair gate
     * had already passed); {@code placeOnCooldown} = of those, the ones the 4-tick BlockPlaceHelper
     * gate swallowed; {@code placeNoHit} = the raytrace was not on a block after all;
     * {@code placeClicked} = {@code interactBlock} returned SUCCESS.
     */
    public static volatile int placeRequested, placeOnCooldown, placeNoHit, placeClicked;

    /**
     * Ticks on which the motion frame ({@link #motionYaw}) differed from the camera by more than
     * {@link #STEER_REPORT_DEG} — i.e. ticks where the bot walked in the direction this movement
     * ASKED for while the camera was still on its way there. Telemetry only, read as
     * {@code placeStats}: "the mechanism fired N times" is the difference between a fix that is
     * doing something and one that merely compiles.
     */
    public static volatile int motionSteered;

    /**
     * The facing in which THIS TICK's inputs are to be resolved, or {@code null} when no ported
     * movement owns the tick. Read by {@code MixinEntityMotionYaw} around
     * {@code Entity.updateVelocity}; cleared at the RETURN of {@code ClientPlayerEntity.tick}, which
     * is upstream's "the target is done being used for this game tick" (LookBehavior.java:126).
     *
     * <p>WHY THIS EXISTS — the mechanism, measured. Baritone's movements press a direction key in the
     * same breath as they ask for a rotation, and that is sound upstream because
     * {@code LookBehavior.onPlayerUpdate} PRE snaps {@code player.setYaw(...)} on that very tick, and
     * {@code MixinEntity} (baritone/src/main/java/baritone/launch/mixins/MixinEntity.java:43-66, and
     * the identical file in shredder/) swaps the yaw around {@code updateVelocity} so the input
     * vector is resolved in the requested facing whatever the camera is doing. Our aim goes through
     * {@link WindMouseRotation}, stepped once per RENDER FRAME, so the direction keys were being
     * resolved in LAST tick's facing. Traced on nav_bridge at the seam where the sneak-backplace
     * (which deliberately faces BACKWARDS down the bridge) hands over to the next step:
     * <pre>
     *   MV 12,-60,0-&gt;13,-60,0 pos=13.30 yaw=90/90   err=0    keys=Su  &lt;- plank placed, facing back
     *   MV 13,-60,0-&gt;14,-60,0 pos=13.30 yaw=90/-90  err=-180 keys=F   &lt;- "forward" pressed...
     *   MV 13,-60,0-&gt;14,-60,0 pos=13.20 yaw=81/-91  err=-171 keys=F   &lt;- ...runs the bot BACKWARDS
     *   MV 13,-60,0-&gt;14,-60,0 pos=12.87 ...
     *   MovementQueue: off path (3.1) at 10.96,-60.00,2.13 ground=true
     * </pre>
     * At 25 fps the turn costs half a block and the course still passes; at the ~9 fps of a full
     * sweep it costs three, the queue gives up, and the bot ends in the void — final_dist 22.5,
     * self_falls=1.
     *
     * <p>MEASURED AND REVERTED, so it is not tried again: holding the direction keys back until the
     * CAMERA reached a forced target changed nothing (22.5 before, 22.5 after, avg_fps 8.8 both).
     * The branch that walks the bot backwards is the {@code MovementHelper.moveTowards} fall-through,
     * whose target is UNFORCED, so the gate never saw it — and widening the gate to every target
     * would make the bot stand still through every heading change, which is not what upstream does.
     * Upstream STEERS. So does this.
     */
    public static volatile Float motionYaw;

    /** Companion to {@link #motionYaw}: the pitch of the same requested rotation. */
    public static volatile Float motionPitch;

    /** {@code LookBehavior}'s POST (LookBehavior.java:100-126): one target, one game tick. */
    public static void clearMotionFrame() {
        motionYaw = null;
        motionPitch = null;
    }

    /** The {@code IPlayerContext} stand-in. See {@link PlayerCtx}. */
    protected final PlayerCtx ctx = new PlayerCtx();

    private MovementState currentState = new MovementState().setStatus(MovementStatus.PREPPING);

    /** Read-only view of the status, for the queue's stuck-scene recorder. Same package only. */
    MovementStatus statusForDiagnostics() {
        return currentState.getStatus();
    }

    protected final BetterBlockPos src;

    protected final BetterBlockPos dest;

    /**
     * The positions that need to be broken before this movement can ensue
     */
    protected final BetterBlockPos[] positionsToBreak;

    /**
     * The position where we need to place a block before this movement can ensue
     */
    protected final BetterBlockPos positionToPlace;

    private Double cost;

    public List<BlockPos> toBreakCached = null;
    public List<BlockPos> toPlaceCached = null;
    public List<BlockPos> toWalkIntoCached = null;

    private Set<BetterBlockPos> validPositionsCached = null;

    private Boolean calculatedWhileLoaded;

    protected Movement(BetterBlockPos src, BetterBlockPos dest, BetterBlockPos[] toBreak, BetterBlockPos toPlace) {
        this.src = src;
        this.dest = dest;
        this.positionsToBreak = toBreak;
        this.positionToPlace = toPlace;
    }

    protected Movement(BetterBlockPos src, BetterBlockPos dest, BetterBlockPos[] toBreak) {
        this(src, dest, toBreak, null);
    }

    public double getCost() throws NullPointerException {
        return cost;
    }

    public double getCost(LevelReader world, LocalPlayer player) {
        if (cost == null) {
            cost = calculateCost(world, player);
        }
        return cost;
    }

    /**
     * Movement.java:91 declares this abstract, taking a {@code CalculationContext}. Tungsten has no
     * such object, so it takes the world and the player instead (spec substitution table). It is
     * left CONCRETE on purpose: a ported movement exposes its price as the upstream STATIC
     * {@code cost(...)} and the planner feeds it in through {@link #override(double)}, so forcing
     * every subclass to also carry an instance form would break compilation for no behaviour. A
     * subclass that wants the cache overrides this.
     */
    public double calculateCost(LevelReader world, LocalPlayer player) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " has no instance cost — call its static cost(...) and override(double) the result");
    }

    public double recalculateCost(LevelReader world, LocalPlayer player) {
        cost = null;
        return getCost(world, player);
    }

    public void override(double cost) {
        this.cost = cost;
    }

    protected abstract Set<BetterBlockPos> calculateValidPositions();

    public Set<BetterBlockPos> getValidPositions() {
        if (validPositionsCached == null) {
            validPositionsCached = calculateValidPositions();
            Objects.requireNonNull(validPositionsCached);
        }
        return validPositionsCached;
    }

    /**
     * Port of {@code PathingBehavior.pathStart()} (baritone/.../behavior/PathingBehavior.java:423-461)
     * — the cell a path is anchored at. It differs from the feet cell only when the feet cell has
     * no floor: standing off the edge of a block, or mid-jump/mid-fall.
     *
     * <p>An earlier comment here claimed tungsten could not have this because "inventing an anchor
     * would be inventing behaviour". That was wrong — the anchor is fully specified upstream and
     * every helper it needs already exists in {@link MovementHelperB}. Note this must NOT be
     * {@code FollowEntityTask.planStart}, which is a structurally similar routine built on
     * {@code PlayerFit} geometry and returning a plain {@code BlockPos}: valid positions are a
     * hash set of {@link BetterBlockPos} with a custom hash, so a plain BlockPos never matches,
     * and the anchor has to be tested with the same block-space predicates the set was built from.
     */
    protected BetterBlockPos pathStart() {
        BetterBlockPos feet = ctx.playerFeet();
        LocalPlayer player = ctx.player();
        if (player == null) {
            return feet;
        }
        net.minecraft.world.level.LevelReader world = player.level();
        if (world == null || MovementHelperB.canWalkOn(world, feet.below())) {
            return feet;
        }
        if (!player.onGround()) {
            // mid-jump
            return MovementHelperB.canWalkOn(world, feet.below().below()) ? feet.below() : feet;
        }
        final double playerX = player.position().x;
        final double playerZ = player.position().z;
        ArrayList<BetterBlockPos> closest = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                closest.add(new BetterBlockPos(feet.x + dx, feet.y, feet.z + dz));
            }
        }
        closest.sort(java.util.Comparator.comparingDouble(pos ->
                ((pos.x + 0.5D) - playerX) * ((pos.x + 0.5D) - playerX)
                        + ((pos.z + 0.5D) - playerZ) * ((pos.z + 0.5D) - playerZ)));
        for (int i = 0; i < 4; i++) {
            BetterBlockPos possibleSupport = closest.get(i);
            double xDist = Math.abs((possibleSupport.x + 0.5D) - playerX);
            double zDist = Math.abs((possibleSupport.z + 0.5D) - playerZ);
            if (xDist > 0.8 && zDist > 0.8) {
                // can't possibly be sneaking off of this one, we're too far away
                continue;
            }
            if (MovementHelperB.canWalkOn(world, possibleSupport.below())
                    && MovementHelperB.canWalkThrough(world, possibleSupport)
                    && MovementHelperB.canWalkThrough(world, possibleSupport.above())) {
                return possibleSupport;   // standing off the edge of a block
            }
        }
        return feet;
    }

    /**
     * Movement.java:112-114. EITHER cell counts — and the second half is what stops a movement
     * declaring UNREACHABLE for the one or two airborne ticks in the middle of its own step.
     * MovementDiagonal is the only live caller and turns a false into an immediate UNREACHABLE
     * that kills the whole chain, which is what the recorded diagonal variance (11 / 19 / 23) was.
     */
    protected boolean playerInValidPosition() {
        return getValidPositions().contains(ctx.playerFeet())
                || getValidPositions().contains(pathStart());
    }

    /** Land movements float toward their destination; a swim controls its own depth. */
    protected void updateWaterInputs(MovementState state) {
        if (MovementHelperB.isLiquid(ctx.world(), ctx.playerFeet())
                && ctx.player().position().y < dest.y + 0.6) {
            state.setInput(Input.JUMP, true);
        }
    }

    /**
     * Handles the execution of the latest Movement
     * State, and offers a Status to the calling class.
     *
     * @return Status
     */
    public MovementStatus update() {
        LocalPlayer player = ctx.player();
        if (player == null) {
            // Not upstream: IPlayerContext.player() is never null while pathing, but
            // MinecraftClient.player is (disconnect, world unload). Returning the unchanged status
            // is the no-op; nothing else in this method is safe without a player.
            return currentState.getStatus();
        }
        player.getAbilities().flying = false;
        currentState = updateState(currentState);
        updateWaterInputs(currentState);
        if (player.isInWall()) {
            ctx.getSelectedBlock().ifPresent(pos -> switchToBestToolFor(pos, ctx.world().getBlockState(pos)));
            // Not when the thing we are wedged against is a door or gate: that is opened, not mined
            // (a held attack key breaks whatever the crosshair is on, and "in wall" against a closed
            // door counted here — this is a door-break path of its own).
            if (!crosshairOnOpenable()) {
                currentState.setInput(Input.CLICK_LEFT, true);
            }
        }

        // If the movement target has to force the new rotations, or we aren't using silent move, then force the rotations
        currentState.getTarget().getRotation().ifPresent(rotation ->
                updateAimTarget(
                        player,
                        rotation,
                        currentState.getTarget().hasToForceRotations()));
        if (kaptainwutax.tungsten.TungstenConfig.get().verboseDebugLogging) {
            logTick(player);
        }
        clearAllKeys();
        // Movement.java:139-148 is `clearAllKeys(); apply; clear; if (complete) clearAllKeys();` —
        // upstream can force the inputs and then take them back, because the force map is consumed
        // a tick later by InputOverrideHandler.onTick. Here the apply IS the tick, and a place click
        // cannot be un-clicked, so the trailing cancellation has to be the guard in front instead.
        // Net behaviour is identical: a completed movement forces no inputs.
        sprintRequested = false;
        if (!currentState.getStatus().isComplete()) {
            armMotionFrame(player);
            sprintRequested = Boolean.TRUE.equals(
                    currentState.getInputStates().get(Input.SPRINT));
            moveTicks++;
            if (sprintRequested) sprintTicks++;
            applyInputs(player, currentState.getInputStates());
        }
        currentState.getInputStates().clear();

        return currentState.getStatus();
    }

    /**
     * Declare the facing this tick's inputs are to be resolved in — the port of
     * {@code LookBehavior.onPlayerRotationMove} (LookBehavior.java:167-173) plus the mixin that
     * consumes it. See {@link #motionYaw} for the trace this was built from.
     *
     * <p>EVERY target counts, forced or not: upstream's hook does not look at the mode either, and
     * the branch that broke nav_bridge is the unforced {@code moveTowards} fall-through. A movement
     * that declares no rotation at all leaves the frame unset and the body walks by the camera, as
     * before.
     */
    private void armMotionFrame(LocalPlayer player) {
        Rotation rotation = currentState.getTarget().getRotation().orElse(null);
        if (rotation == null) {
            return;
        }
        motionYaw = rotation.getYaw();
        motionPitch = rotation.getPitch();
        if (Math.abs(Mth.wrapDegrees(rotation.getYaw() - player.getYRot())) > STEER_REPORT_DEG) {
            motionSteered++;
        }
    }

    /**
     * DIAGNOSTIC, verbose-gated, one line per ticked movement — never in a search loop. It prints the
     * three things a tick of this port is made of, together, because every mechanism question asked
     * of it so far ("did it press MOVE_BACK before or after the camera turned round?") needs all
     * three at once: the body (feet cell + exact position), the CAMERA (where it is against where
     * this tick asked it to be) and the KEYS this tick declared. A crossing is ~250 ticks, so the
     * whole run fits in the container log.
     *
     * <p>READ IT FROM {@code docker logs uctest-mc-tester1}, not from {@code getRecentChat}: 250
     * lines overflow the py4j chat ring ({@code Py4jEntryPoint.CHAT_LOG_MAX = 300}) and the client
     * prints "Chat overflow, message dropped". Everything sent to chat also reaches the container's
     * stdout as a {@code [CHAT]} line, which has no such limit.
     */
    private void logTick(LocalPlayer player) {
        Rotation want = currentState.getTarget().getRotation().orElse(null);
        float yawNow = player.getYRot();
        String yaw = want == null
                ? String.format("yaw=%.0f/- err=-", yawNow)
                : String.format("yaw=%.0f/%.0f err=%.0f", yawNow, want.getYaw(),
                        Mth.wrapDegrees(want.getYaw() - yawNow));
        StringBuilder keys = new StringBuilder();
        for (Map.Entry<Input, Boolean> e : currentState.getInputStates().entrySet()) {
            if (!Boolean.TRUE.equals(e.getValue())) {
                continue;
            }
            switch (e.getKey()) {
                case MOVE_FORWARD: keys.append('F'); break;
                case MOVE_BACK:    keys.append('B'); break;
                case MOVE_LEFT:    keys.append('L'); break;
                case MOVE_RIGHT:   keys.append('R'); break;
                case JUMP:         keys.append('J'); break;
                case SNEAK:        keys.append('S'); break;
                case SPRINT:       keys.append('P'); break;
                case CLICK_LEFT:   keys.append('a'); break;
                case CLICK_RIGHT:  keys.append('u'); break;
                default: break;
            }
        }
        Vec3 p = player.position();
        kaptainwutax.tungsten.Debug.logMessage(String.format(
                "MV %s->%s st=%s feet=%s pos=%.2f,%.2f,%.2f %s keys=%s ground=%b pose=%b",
                src.toString(), dest.toString(), currentState.getStatus(),
                ctx.playerFeet().toString(), p.x, p.y, p.z, yaw,
                keys.length() == 0 ? "-" : keys.toString(),
                player.onGround(), player.isCrouching()));
    }

    /** Consecutive prepping ticks spent clicking at a block we cannot aim at. */
    private int blindPrepTicks = 0;

    /**
     * Three seconds -- past any honest aim-and-swing, and under the six after which MovementQueue
     * abandons the whole chain, so this speaks first and the planner gets to route around instead
     * of returning the identical route.
     */
    private static final int BLIND_PREP_LIMIT = 60;

    /** Movements that gave up because the thing in the way could never be aimed at. */
    public static volatile int blindPrepGaveUp;

    /**
     * True when the GAME's live crosshair is on a door or fence gate. Those are opened, not mined.
     *
     * <p>The blind fallback in {@link #prepared(MovementState)} deliberately holds CLICK_LEFT at
     * whatever the crosshair is on ("breaking the incorrect block"), and the reachable branch trusts
     * tungsten's own raytrace, which can disagree with the game's. Either way a held attack key
     * breaks the block under the crosshair — so when that block is a door or gate the bot mines a
     * door it should have opened. Refuse the swing; the blind counter in the caller still pushes the
     * movement to UNREACHABLE, so the planner re-plans instead of standing there.
     */
    protected boolean crosshairOnOpenable() {
        net.minecraft.world.phys.HitResult hit = net.minecraft.client.Minecraft.getInstance().hitResult;
        if (!(hit instanceof net.minecraft.world.phys.BlockHitResult bhr)
                || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return false;
        }
        net.minecraft.world.level.block.Block block = ctx.world().getBlockState(bhr.getBlockPos()).getBlock();
        // An IRON door is not openable by hand, so it is a legitimate thing to mine -- refusing the
        // swing there only made the bot stand at one. Only the openables are refused.
        return (block instanceof net.minecraft.world.level.block.DoorBlock
                        && block != net.minecraft.world.level.block.Blocks.IRON_DOOR)
                || block instanceof net.minecraft.world.level.block.FenceGateBlock;
    }

    /**
     * May this cell be OPENED by the movement instead of mined?
     *
     * <p>Default no: most movements have no use-click and an obstruction is an obstruction.
     * Overridden by the movement that knows how to open it ({@code MovementTraverse}, which carries
     * the door/gate CLICK_RIGHT), so {@link #prepared} can step over the cell and let that movement
     * reach its own logic. See the comment at the step-over site for the symptom this fixes.
     */
    protected boolean canOpenInsteadOfBreak(BetterBlockPos pos) {
        return false;
    }

    protected boolean prepared(MovementState state) {
        if (state.getStatus() == MovementStatus.WAITING) {
            return true;
        }
        boolean somethingInTheWay = false;
        for (BetterBlockPos blockPos : positionsToBreak) {
            if (!ctx.world().getEntitiesOfClass(FallingBlockEntity.class, new AABB(0, 0, 0, 1, 1.1, 1).move(blockPos)).isEmpty() && PAUSE_MINING_FOR_FALLING_BLOCKS) {
                return false;
            }
            // ...AND YOU CANNOT BREAK A FLUID EITHER. Flowing water fails canWalkThrough, so it
            // lands here and this loop aims at it and holds CLICK_LEFT forever: the block never
            // goes away, prepared() never returns true, and because Movement.updateState returns
            // PREPPING before every subclass's own logic the step also never arrives and never
            // fails. It just sits until the queue's cost+100 timeout. Measured on chase_terrain:
            // the same flat step (-157,60,288)->(-156,60,288) timing out every eight seconds, and
            // rcon says the bot's feet, its head AND the destination are all minecraft:water.
            //
            // Whether a route should go through water is the PLANNER's business. What the
            // executor must not do is spend eight seconds mining something that cannot be mined.
            if (!ctx.world().getFluidState(blockPos).isEmpty()) {
                continue;
            }
            // ⛔ A DOOR OR GATE IN THE WAY IS OPENED, NOT MINED — AND IT IS NOT AN OBSTRUCTION HERE
            // EITHER. This loop used to treat a closed door as something to mine, and mining it was
            // the ONLY way the bot ever got through one. When that swing was refused (see
            // #crosshairOnOpenable) the movement never left PREPPING — and a movement stuck in
            // PREPPING never reaches its own door logic, because MovementTraverse.updateState runs
            // its CLICK_RIGHT branch only once the status is RUNNING. Symptom: the bot walks up to a
            // closed door and stands there. So a cell the movement can OPEN is stepped over exactly
            // as if it were walkable-through — which is what the movement is about to make true.
            if (canOpenInsteadOfBreak(blockPos) && !MovementHelperB.canWalkThrough(ctx.world(), blockPos)) {
                continue;
            }
            if (!MovementHelperB.canWalkThrough(ctx.world(), blockPos)) { // can't break air, so don't try
                somethingInTheWay = true;
                switchToBestToolFor(blockPos, ctx.world().getBlockState(blockPos));
                Optional<Rotation> reachable = RotationHelper.reachable(ctx.player(), blockPos, ctx.blockReachDistance());
                if (reachable.isPresent()) {
                    Rotation rotTowardsBlock = reachable.get();
                    state.setTarget(new MovementState.MovementTarget(rotTowardsBlock, true));
                    if ((ctx.isLookingAt(blockPos) || ctx.playerRotations().isReallyCloseTo(rotTowardsBlock))
                            && !crosshairOnOpenable()) {
                        state.setInput(Input.CLICK_LEFT, true);
                    }
                    return false;
                }
                // ⛔⛔ CLICKING AT A BLOCK WE CANNOT EVEN AIM AT IS THE LIVELOCK, AND THE BRANCH
                // MEANT TO CATCH IT IS DEAD CODE. `somethingInTheWay` is set above and BOTH paths
                // return false, so the `if (somethingInTheWay) -> UNREACHABLE` after the loop can
                // only be reached with the flag still false -- checked against all three
                // occurrences in this file. A movement can never report UNREACHABLE for an
                // obstruction it cannot clear.
                //
                // Measured cost: reachable is EMPTY here, so we aim at the block centre and hold
                // CLICK_LEFT at something the crosshair does not land on -- the same blocked ray
                // the captures show as rayOther=3091, blockedBy=minecraft:grass_block. The block
                // never breaks, prepared() never returns true, and since updateState returns
                // before every subclass's logic the step neither arrives nor fails. Six seconds
                // later qNoMove drops the chain, the planner returns the identical route, and the
                // run is spent on that loop: 44 qNoMove per zero-rung run against 0 per scoring
                // one, the body never leaving a seven-block patch at one altitude.
                //
                // Mining a block we CAN see is untouched. Only the blind case changes, and it
                // changes to what the dead branch intended.
                blindPrepTicks++;
                if (kaptainwutax.tungsten.TungstenConfig.get().prepFailsWhenBlind
                        && blindPrepTicks > BLIND_PREP_LIMIT) {
                    blindPrepGaveUp++;
                    state.setStatus(MovementStatus.UNREACHABLE);
                    return true;
                }
                //get rekt minecraft
                //i'm doing it anyway
                //i dont care if theres snow in the way!!!!!!!
                //you dont own me!!!!
                state.setTarget(new MovementState.MovementTarget(RotationHelper.calcRotationFromVec3d(ctx.playerHead(),
                        RotationHelper.getBlockPosCenter(blockPos), ctx.playerRotations()), true)
                );
                // don't check selectedblock on this one, this is a fallback when we can't see any face directly, it's intended to be breaking the "incorrect" block
                // — but NOT a door or gate: those are opened, not mined, and a held attack key breaks
                // whatever the crosshair is on (this is exactly how a closed door got mined).
                if (!crosshairOnOpenable()) {
                    state.setInput(Input.CLICK_LEFT, true);
                }
                return false;
            }
        }
        if (somethingInTheWay) {
            // There's a block or blocks that we can't walk through, but we have no target rotation to reach any
            // So don't return true, actually set state to unreachable
            state.setStatus(MovementStatus.UNREACHABLE);
            return true;
        }
        return true;
    }

    public boolean safeToCancel() {
        return safeToCancel(currentState);
    }

    protected boolean safeToCancel(MovementState currentState) {
        return true;
    }

    public BetterBlockPos getSrc() {
        return src;
    }

    public BetterBlockPos getDest() {
        return dest;
    }

    public void reset() {
        currentState = new MovementState().setStatus(MovementStatus.PREPPING);
    }

    /**
     * Calculate latest movement state. Gets called once a tick.
     *
     * @param state The current state
     * @return The new state
     */
    public MovementState updateState(MovementState state) {
        if (!prepared(state)) {
            return state.setStatus(MovementStatus.PREPPING);
        } else if (state.getStatus() == MovementStatus.PREPPING) {
            state.setStatus(MovementStatus.WAITING);
        }

        if (state.getStatus() == MovementStatus.WAITING) {
            state.setStatus(MovementStatus.RUNNING);
        }

        return state;
    }

    public BlockPos getDirection() {
        return getDest().subtract(getSrc());
    }

    /** Movement.java:244-246. {@code bsi.worldContainsLoadedChunk(x, z)} is
     *  {@code provider.isChunkLoaded(x >> 4, z >> 4)} (BlockStateInterface.java:79-81). */
    public void checkLoadedChunk(LevelReader world) {
        calculatedWhileLoaded = world.hasChunk(dest.x >> 4, dest.z >> 4);
    }

    public boolean calculatedWhileLoaded() {
        return calculatedWhileLoaded;
    }

    /**
     * Does this movement REQUIRE its {@code positionsToBreak} to be clear before it can run?
     *
     * <p>True for everything that gates on {@code prepared()}, which is the default. False for the
     * movements whose {@code prepared()} is unconditionally true — a diagonal never breaks its
     * corners (upstream prices a diagonal with ONE solid corner as walkable and edges around it),
     * and a fallback steer breaks nothing at all. Both nevertheless DECLARE cells in
     * {@code positionsToBreak}, so a chain vetted on {@code toBreak} alone was being cut at every
     * diagonal that hugs a wall — the common case on terrain, and precisely where the diagonals
     * were meant to add coverage.
     */
    public boolean needsClearBreaks() {
        return true;
    }

    /**
     * Did the movement just executed ask for SPRINT? Read by {@link MovementQueue} straight after
     * {@code update()}, because the input map is CLEARED at the end of it — the queue cannot ask
     * the state afterwards, and asking before would race the movement's own decision.
     */
    public boolean sprintRequested() {
        return sprintRequested;
    }

    /** Ticks a queued movement asked for SPRINT, and ticks one ran at all. A chase that keeps
     *  station 25 blocks behind a sprinting target is a SPEED question, and speed is not
     *  something to A/B on a bimodal metric — it is something to count. */
    public static volatile int sprintTicks, moveTicks;

    private boolean sprintRequested = false;

    public void resetBlockCache() {
        toBreakCached = null;
        toPlaceCached = null;
        toWalkIntoCached = null;
    }

    public List<BlockPos> toBreak(LevelReader bsi) {
        if (toBreakCached != null) {
            return toBreakCached;
        }
        List<BlockPos> result = new ArrayList<>();
        for (BetterBlockPos positionToBreak : positionsToBreak) {
            if (!MovementHelperB.canWalkThrough(bsi, positionToBreak.x, positionToBreak.y, positionToBreak.z)) {
                result.add(positionToBreak);
            }
        }
        toBreakCached = result;
        return result;
    }

    public List<BlockPos> toPlace(LevelReader bsi) {
        if (toPlaceCached != null) {
            return toPlaceCached;
        }
        List<BlockPos> result = new ArrayList<>();
        if (positionToPlace != null && !MovementHelperB.canWalkOn(bsi, positionToPlace.x, positionToPlace.y, positionToPlace.z)) {
            result.add(positionToPlace);
        }
        toPlaceCached = result;
        return result;
    }

    public List<BlockPos> toWalkInto(LevelReader bsi) { // overridden by movementdiagonal
        if (toWalkIntoCached == null) {
            toWalkIntoCached = new ArrayList<>();
        }
        return toWalkIntoCached;
    }

    public BlockPos[] toBreakAll() {
        return positionsToBreak;
    }

    // ------------------------------------------------------------------------------------------
    // Adapters. Everything below stands in for a baritone service tungsten does not have; none of
    // it adds a decision the movement did not make.
    // ------------------------------------------------------------------------------------------

    /**
     * {@code MovementHelper.switchToBestToolFor(ctx, state)} (MovementHelper.java:698-713), which
     * ends in {@code player.getInventory().selectedSlot = ts.getBestSlot(...)}. Tungsten never
     * touches the inventory — altoclef owns the hotbar and registers
     * {@code equipToolHook} for exactly this (TungstenModDataContainer.java:22-30) — so the hook is
     * the port and baritone's {@code ToolSet} is not carried over. The hook must never be able to
     * break mining, hence the swallow (same convention as PathExecutor.java:398-403).
     */
    private static void switchToBestToolFor(BlockPos pos, BlockState state) {
        if (TungstenModDataContainer.equipToolHook != null) {
            try {
                TungstenModDataContainer.equipToolHook.accept(pos, state);
            } catch (Throwable ignored) {}
        }
    }

    /**
     * {@code LookBehavior.updateTarget(rotation, force)} (Movement.java:135-138). Forced targets are
     * the ones a break/place gate will be tested against, so they get the full aim; the unforced one
     * is {@code MovementHelper.moveTowards} (MovementHelper.java:715-722), where the pitch must stay
     * where the player left it — a walk must not tilt the camera. Never {@code setYaw}/{@code setPitch}:
     * WindMouseRotation feeds pixel deltas through the vanilla mouse pipeline, so the server sees a
     * mouse, not a teleport.
     */
    private static void updateAimTarget(LocalPlayer player, Rotation rotation, boolean force) {
        if (force) {
            WindMouseRotation.INSTANCE.setTarget(rotation.getYaw(), rotation.getPitch());
        } else {
            WindMouseRotation.INSTANCE.setTargetFast(rotation.getYaw(), player.getXRot());
        }
    }

    /**
     * {@code InputOverrideHandler.clearAllKeys()} (InputOverrideHandler.java:80-83). Upstream clears
     * a force map that mixins read; here the keys themselves are released, which is the same thing
     * one layer down. Called before every apply, so no input can latch across ticks, and again on a
     * completed movement (Movement.java:146-148).
     *
     * <p>Package-visible, not private: upstream's {@code clearAllKeys} is public API on
     * {@code InputOverrideHandler} and {@code PathExecutor.onLostControl} calls it when it hands the
     * body back (PathExecutor.java:346). {@link MovementQueue#stop()} is that call here — a queue
     * that stops mid-sneak must not leave SHIFT latched.
     */
    static void clearAllKeys() {
        Options options = Minecraft.getInstance().options;
        if (options == null) {
            return;
        }
        options.keyUp.setDown(false);
        options.keyDown.setDown(false);
        options.keyLeft.setDown(false);
        options.keyRight.setDown(false);
        options.keyJump.setDown(false);
        options.keyShift.setDown(false);
        options.keySprint.setDown(false);
        options.keyAttack.setDown(false);
        // CLICK_RIGHT is not a key here (see blockPlaceHelperTick), but release useKey anyway so
        // this stays the only owner of the inputs for the tick.
        options.keyUse.setDown(false);
    }

    /**
     * The press half of Movement.java:139-143 — exactly what this tick declared, nothing else.
     *
     * <p>The two clicks are not keys: {@code CLICK_LEFT} is the attack key held down (vanilla runs
     * the break progress), while {@code CLICK_RIGHT} goes through the {@code BlockPlaceHelper} gate
     * because a use-key press would place on every tick and out of the player's real crosshair.
     * {@code CLICK_LEFT} forced on a tick cancels {@code CLICK_RIGHT} (InputOverrideHandler.java:94-96)
     * — breaking and placing never overlap — and that has to be decided BEFORE anything is applied,
     * since the map's iteration order is not the branch order.
     */
    private void applyInputs(LocalPlayer player, Map<Input, Boolean> inputStates) {
        Options options = Minecraft.getInstance().options;
        if (options == null) {
            return;
        }
        boolean clickLeft = Boolean.TRUE.equals(inputStates.get(Input.CLICK_LEFT));
        boolean clickRight = Boolean.TRUE.equals(inputStates.get(Input.CLICK_RIGHT));
        if (clickLeft) {
            clickRight = false;
        }
        inputStates.forEach((input, forced) -> {
            switch (input) {
                case MOVE_FORWARD:
                    options.keyUp.setDown(forced);
                    break;
                case MOVE_BACK:
                    options.keyDown.setDown(forced);
                    break;
                case MOVE_LEFT:
                    options.keyLeft.setDown(forced);
                    break;
                case MOVE_RIGHT:
                    options.keyRight.setDown(forced);
                    break;
                case JUMP:
                    options.keyJump.setDown(forced);
                    break;
                case SNEAK:
                    options.keyShift.setDown(forced);
                    break;
                case SPRINT:
                    options.keySprint.setDown(forced);
                    break;
                case CLICK_LEFT:
                case CLICK_RIGHT:
                    // handled below, after the interlock
                    break;
                default:
                    break;
            }
        });
        options.keyAttack.setDown(clickLeft);
        blockPlaceHelperTick(player, clickRight);
    }

    /**
     * {@code BlockPlaceHelper.tick} (BlockPlaceHelper.java:38-57): one attempt every four ticks, and
     * the cooldown is charged even when the attempt fails. The gate itself now lives in
     * {@code helpers/BlockPlaceHelper} and is ticked once per client tick, as upstream ticks it
     * from the input handler — so the rate is shared with every other placement site instead of
     * applying to ported movements alone.
     *
     * <p>The hit result handed to {@code interactBlock} is THE ONE THE RAYTRACE PRODUCED — never a
     * {@link BlockHitResult} built from a face centre, which would be a packet claiming a click the
     * player never made. Nor is there a target position to check here: the movement's own
     * {@code attemptToPlaceABlock} already refused to ask for {@code CLICK_RIGHT} until the real
     * crosshair agreed (MovementHelper.java:840-851), so a second gate would only re-implement it.
     */
    private void blockPlaceHelperTick(LocalPlayer player, boolean rightClickRequested) {
        if (rightClickRequested) {
            placeRequested++;
        }
        if (kaptainwutax.tungsten.helpers.BlockPlaceHelper.onCooldown()) {
            if (rightClickRequested) {
                placeOnCooldown++;
            }
            return;
        }
        HitResult mouseOver = ctx.objectMouseOver();
        if (!rightClickRequested || player.isHandsBusy() || mouseOver == null || mouseOver.getType() != HitResult.Type.BLOCK) {
            if (rightClickRequested) {
                placeNoHit++;
            }
            return;
        }
        if (kaptainwutax.tungsten.helpers.BlockPlaceHelper.tryPlace((BlockHitResult) mouseOver)) {
            placeClicked++;
        }
    }

    /**
     * {@code IPlayerContext} (IPlayerContext.java:62-120) reduced to what the ported movements
     * actually read. Deliberately not a general-purpose facade: the point of the adapter is that a
     * verbatim {@code ctx.playerFeet()} / {@code ctx.isLookingAt(pos)} from upstream compiles here
     * and means the same thing.
     */
    protected static final class PlayerCtx {

        public LocalPlayer player() {
            return Minecraft.getInstance().player;
        }

        /** {@code ctx.world()}. ClientWorld rather than WorldView because {@link #prepared} needs
         *  {@code getNonSpectatingEntities}; it is still a WorldView everywhere else. */
        public ClientLevel world() {
            return Minecraft.getInstance().level;
        }

        /**
         * IPlayerContext.java:62-79, verbatim. The {@code +0.1251} and the slab correction are not
         * cosmetic: a plain floor of y reports the wrong cell on landing ticks, which is exactly
         * when SUCCESS is tested. ({@code feet.above()} is tungsten's BetterBlockPos-typed
         * {@code up()} — BetterBlockPos.java:107.)
         */
        public BetterBlockPos playerFeet() {
            LocalPlayer player = player();
            BetterBlockPos feet = new BetterBlockPos(player.position().x, player.position().y + 0.1251, player.position().z);
            try {
                if (world().getBlockState(feet).getBlock() instanceof SlabBlock) {
                    return feet.above();
                }
            } catch (NullPointerException ignored) {}
            return feet;
        }

        /**
         * IPlayerContext.java:93-95. NOT {@code getEyePos()}: that folds in the current pose, which
         * destroys the standing-vs-sneaking eye separation (1.62 vs 1.27) the backplace pitch is
         * built on.
         */
        public Vec3 playerHead() {
            LocalPlayer player = player();
            return new Vec3(player.position().x, player.position().y + player.getEyeHeight(), player.position().z);
        }

        public Rotation playerRotations() {
            LocalPlayer player = player();
            return new Rotation(player.getYRot(), player.getXRot());
        }

        /**
         * BaritonePlayerContext.java:84-86 — a LIVE raytrace recomputed this tick from the real eye
         * and the real current rotations, not the per-frame {@code mc.crosshairTarget}.
         * {@code Entity.raycast(reach, 1.0F, false)} is that raytrace exactly: it starts at
         * {@code getCameraPosVec}, aims along the current rotations, and uses
         * {@code ShapeType.OUTLINE} with {@code FluidHandling.NONE} — the same three choices as
         * RayTraceUtils.java:33-48.
         */
        public HitResult objectMouseOver() {
            return player().pick(blockReachDistance(), 1.0F, false);
        }

        public Optional<BlockPos> getSelectedBlock() {
            HitResult result = objectMouseOver();
            if (result != null && result.getType() == HitResult.Type.BLOCK) {
                return Optional.of(((BlockHitResult) result).getBlockPos());
            }
            return Optional.empty();
        }

        public boolean isLookingAt(BlockPos pos) {
            return getSelectedBlock().equals(Optional.of(pos));
        }

        /** {@code playerController().getBlockReachDistance()} (IPlayerController.java:58-60). */
        public double blockReachDistance() {
            return player().isCreative() ? 5.0 : BLOCK_REACH_DISTANCE;
        }
    }
}

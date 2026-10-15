package adris.altoclef.control;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.Settings;
import adris.altoclef.util.goals.AltoGoal;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Optional bridge to the separate <b>Baritone</b> mod, so every UnionClef command can be served by
 * either engine.
 *
 * <h2>What this is for</h2>
 *
 * The GUI lets the player pick a movement engine: {@code Tungsten} (this mod's built-in pathfinder)
 * or {@code Baritone} (the standalone mod, driven with its own {@code #} chat commands). Selecting
 * Baritone cannot be a GUI-only choice — {@code @get}, {@code @goto}, {@code @hero} and every other
 * command move the bot through {@link adris.altoclef.tasks.movement.CustomBaritoneGoalTask}, so the
 * goal has to be handed to whichever engine is chosen. That hand-off happens here and in the one
 * task, and nowhere else: the rest of the mod keeps stating intent in {@link AltoGoal} terms and
 * never learns which engine is behind it.
 *
 * <h2>Why reflection, not a compile dependency</h2>
 *
 * Baritone is a normal mod jar the player installs, not a build-time library of this project. Linking
 * against it would make the jar a hard build requirement and ship 4.8 MB of someone else's code in
 * this repository. Reflection keeps Baritone genuinely optional: without it installed every method
 * answers "no" and the mod runs exactly as before on Tungsten.
 *
 * <h2>What it deliberately does not do</h2>
 *
 * It does not configure Baritone. The GUI sends ordinary {@code #set} chat lines for that, so the
 * settings surface is Baritone's own and cannot drift out of step with a table copied here.
 */
public final class BaritoneBridge {

    private BaritoneBridge() {
    }

    /** Engine name stored in {@link Settings#getPathfinder()} that selects Baritone. */
    public static final String ENGINE = "Baritone";

    /** Goals handed to Baritone, and ones it could not be given (no instance, unknown shape). */
    public static volatile int bPathRequests, bPathFails;

    private static volatile boolean probed = false;
    private static volatile boolean ok = false;

    private static Method mGetProvider;        // BaritoneAPI.getProvider()
    private static Method mGetPrimary;         // IBaritoneProvider.getPrimaryBaritone()
    private static Method mGetCustomGoal;      // IBaritone.getCustomGoalProcess()
    private static Method mGetPathing;         // IBaritone.getPathingBehavior()
    private static Method mGetFollow;          // IBaritone.getFollowProcess()
    private static Method mGetMine;            // IBaritone.getMineProcess()
    private static Method mMineBlocks;         // IMineProcess.mine(int, Block...)
    private static Method mProcessActive;      // IBaritoneProcess.isActive()
    private static Method mCancelProcess;      // IBaritoneProcess/IMineProcess.cancel()
    private static Method mDestroy;            // IBaritoneProvider.destroyBaritone(IBaritone)
    private static Method mSetGoal;            // ICustomGoalProcess.setGoal(Goal)
    private static Method mPath;               // ICustomGoalProcess.path()
    private static Method mPathing;            // IPathingBehavior.isPathing()
    private static Method mCancelEverything;   // IPathingBehavior.cancelEverything()
    private static Method mGetGoal;            // IPathingBehavior.getGoal()
    private static Method mFollow;             // IFollowProcess.follow(Predicate)
    private static Method mFollowing;          // IFollowProcess.following()
    private static Method mPickup;             // IFollowProcess.pickup(Predicate<ItemStack>)
    private static Method mFollowCancel;       // IFollowProcess.cancel()
    private static Method mGetSettings;        // BaritoneAPI.getSettings()
    private static Class<?> settingClass;      // baritone.api.Settings$Setting
    private static java.lang.reflect.Field fValue;   // Setting.value

    private static Class<?> goalIface;
    private static Constructor<?> cGoalBlock, cGoalNear, cGoalGetToBlock, cGoalXZ, cGoalYLevel,
            cGoalRunAway, cGoalComposite;

    /**
     * Resolve Baritone's classes once. A failure is not an error — it is the ordinary case where the
     * player did not install the mod — so it is logged at message level and remembered.
     */
    private static synchronized void probe() {
        if (probed) return;
        probed = true;
        try {
            Class<?> api = Class.forName("baritone.api.BaritoneAPI");
            mGetProvider = api.getMethod("getProvider");
            mGetPrimary = Class.forName("baritone.api.IBaritoneProvider").getMethod("getPrimaryBaritone");
            mDestroy = Class.forName("baritone.api.IBaritoneProvider")
                    .getMethod("destroyBaritone", Class.forName("baritone.api.IBaritone"));

            Class<?> iBaritone = Class.forName("baritone.api.IBaritone");
            mGetCustomGoal = iBaritone.getMethod("getCustomGoalProcess");
            mGetPathing = iBaritone.getMethod("getPathingBehavior");
            mGetFollow = iBaritone.getMethod("getFollowProcess");
            mGetMine = iBaritone.getMethod("getMineProcess");
            Class<?> imp = Class.forName("baritone.api.process.IMineProcess");
            mMineBlocks = imp.getMethod("mine", int.class, net.minecraft.world.level.block.Block[].class);
            mProcessActive = Class.forName("baritone.api.process.IBaritoneProcess").getMethod("isActive");
            mCancelProcess = imp.getMethod("cancel");

            goalIface = Class.forName("baritone.api.pathing.goals.Goal");
            Class<?> cgp = Class.forName("baritone.api.process.ICustomGoalProcess");
            mSetGoal = cgp.getMethod("setGoal", goalIface);
            mPath = cgp.getMethod("path");

            Class<?> pb = Class.forName("baritone.api.behavior.IPathingBehavior");
            mPathing = pb.getMethod("isPathing");
            mCancelEverything = pb.getMethod("cancelEverything");
            mGetGoal = pb.getMethod("getGoal");

            Class<?> fp = Class.forName("baritone.api.process.IFollowProcess");
            mFollow = fp.getMethod("follow", java.util.function.Predicate.class);
            mFollowing = fp.getMethod("following");
            mPickup = fp.getMethod("pickup", java.util.function.Predicate.class);
            mFollowCancel = fp.getMethod("cancel");

            mGetSettings = api.getMethod("getSettings");
            settingClass = Class.forName("baritone.api.Settings$Setting");
            fValue = settingClass.getField("value");

            cGoalBlock = Class.forName("baritone.api.pathing.goals.GoalBlock")
                    .getConstructor(int.class, int.class, int.class);
            cGoalNear = Class.forName("baritone.api.pathing.goals.GoalNear")
                    .getConstructor(BlockPos.class, int.class);
            cGoalGetToBlock = Class.forName("baritone.api.pathing.goals.GoalGetToBlock")
                    .getConstructor(BlockPos.class);
            cGoalXZ = Class.forName("baritone.api.pathing.goals.GoalXZ")
                    .getConstructor(int.class, int.class);
            cGoalYLevel = Class.forName("baritone.api.pathing.goals.GoalYLevel")
                    .getConstructor(int.class);
            cGoalRunAway = Class.forName("baritone.api.pathing.goals.GoalRunAway")
                    .getConstructor(double.class, BlockPos[].class);
            cGoalComposite = Class.forName("baritone.api.pathing.goals.GoalComposite")
                    .getConstructor(goalIface.arrayType());

            ok = true;
            // Register with tungsten's movement delegate so EVERY task's movement -- including the
            // walks and pillars tungsten starts for itself (the build queue, EndermanShelterTask) --
            // goes to the engine the player chose. See kaptainwutax.tungsten.api.MovementDelegates.
            kaptainwutax.tungsten.api.MovementDelegates.set(new Delegate());
            Debug.logMessage("Baritone bridge ready.");
        } catch (Throwable t) {
            ok = false;
            Debug.logMessage("Baritone mod not installed (or API changed); the Baritone engine is unavailable.");
        }
    }

    /** Is the Baritone mod present and its API usable? */
    public static boolean available() {
        probe();
        return ok;
    }

    /** Is Baritone the engine the player chose? */
    public static boolean selected() {
        if (!available()) return false;
        AltoClef mod = AltoClef.getInstance();
        Settings s = mod == null ? null : mod.getModSettings();
        return s != null && ENGINE.equalsIgnoreCase(s.getPathfinder());
    }

    /** The live IBaritone for this connection, or null when there is none (title screen, no mod). */
    private static Object baritone() {
        if (!ok) return null;
        try {
            Object provider = mGetProvider.invoke(null);
            return provider == null ? null : mGetPrimary.invoke(provider);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object pathingBehavior(Object bar) throws Exception {
        return mGetPathing.invoke(bar);
    }

    public static boolean isPathing() {
        Object bar = baritone();
        if (bar == null) return false;
        try {
            return Boolean.TRUE.equals(mPathing.invoke(pathingBehavior(bar)));
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean hasGoal() {
        Object bar = baritone();
        if (bar == null) return false;
        try {
            return mGetGoal.invoke(pathingBehavior(bar)) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Stop the current goal and route. */
    public static void cancel() {
        Object bar = baritone();
        if (bar == null) return;
        try {
            mCancelEverything.invoke(pathingBehavior(bar));
        } catch (Throwable ignored) {
            // a stop that fails is not worth killing a tick over
        }
    }

    /**
     * Follow a player by name with Baritone's own follow process — the engine-specific equivalent of
     * {@code @follow}, which under tungsten is served by its dedicated follow walker rather than by
     * a goal.
     */
    public static boolean followPlayer(String name) {
        Object bar = baritone();
        if (bar == null || name == null) return false;
        try {
            Object fp = mGetFollow.invoke(bar);
            java.util.function.Predicate<net.minecraft.world.entity.Entity> target =
                    e -> e instanceof net.minecraft.world.entity.player.Player p
                            && name.equalsIgnoreCase(p.getName().getString());
            mFollow.invoke(fp, target);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Is Baritone currently following the named player? */
    public static boolean isFollowingPlayer(String name) {
        Object bar = baritone();
        if (bar == null || name == null) return false;
        try {
            Object list = mFollowing.invoke(mGetFollow.invoke(bar));
            if (list instanceof java.util.List<?> following) {
                for (Object e : following) {
                    if (e instanceof net.minecraft.world.entity.player.Player p
                            && name.equalsIgnoreCase(p.getName().getString())) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Stop Baritone's follow process. */
    public static void cancelFollow() {
        Object bar = baritone();
        if (bar == null) return;
        try {
            mFollowCancel.invoke(mGetFollow.invoke(bar));
        } catch (Throwable ignored) {
            // a stop that fails is not worth killing a tick over
        }
    }

    /**
     * Hand an altoclef goal to Baritone.
     *
     * @return true when the goal was accepted
     */
    public static boolean pathTo(AltoGoal goal) {
        if (goal == null) return false;
        Object bar = baritone();
        if (bar == null) return false;
        try {
            Object bGoal = toBaritone(goal);
            if (bGoal == null) {
                bPathFails++;
                return false;
            }
            Object cgp = mGetCustomGoal.invoke(bar);
            mSetGoal.invoke(cgp, bGoal);
            mPath.invoke(cgp);
            bPathRequests++;
            return true;
        } catch (Throwable t) {
            bPathFails++;
            return false;
        }
    }

    /**
     * Translate altoclef's goal vocabulary into Baritone's.
     *
     * <p>The common places get Baritone's own tuned goal classes, so its A* keeps the heuristic the
     * class was written to provide. Region and predicate goals — which have no equivalent — go
     * through {@link #proxyGoal(AltoGoal)}, a runtime {@code Goal} answered by {@link AltoGoal}'s own
     * {@code reached} test: one adapter covers every shape, including ones added later.
     */
    private static Object toBaritone(AltoGoal goal) throws Exception {
        if (goal instanceof AltoGoal.Block b) {
            return cGoalBlock.newInstance(b.pos().getX(), b.pos().getY(), b.pos().getZ());
        }
        if (goal instanceof AltoGoal.Near n) {
            return cGoalNear.newInstance(n.pos(), n.range());
        }
        if (goal instanceof AltoGoal.Adjacent a) {
            return cGoalGetToBlock.newInstance(a.pos());
        }
        if (goal instanceof AltoGoal.Xz xz) {
            return cGoalXZ.newInstance(xz.x(), xz.z());
        }
        if (goal instanceof AltoGoal.YLevel y) {
            return cGoalYLevel.newInstance(y.y());
        }
        if (goal instanceof AltoGoal.Chunk c) {
            // Baritone has no chunk goal; its centre column is the equivalent instruction, and
            // altoclef's own isFinished still decides arrival on the full 16x16.
            return cGoalXZ.newInstance(c.startX() + 8, c.startZ() + 8);
        }
        if (goal instanceof AltoGoal.Direction d) {
            Vec3 t = d.target();
            return cGoalXZ.newInstance((int) Math.floor(t.x), (int) Math.floor(t.z));
        }
        if (goal instanceof AltoGoal.AnyBlock ab) {
            var cells = ab.cells();
            int n = Math.min(cells.size(), 256);
            Object arr = Array.newInstance(goalIface, n);
            int i = 0;
            for (BlockPos p : cells) {
                if (i >= n) break;
                Array.set(arr, i++, cGoalBlock.newInstance(p.getX(), p.getY(), p.getZ()));
            }
            if (n == 1) return Array.get(arr, 0);
            return cGoalComposite.newInstance(arr);
        }
        if (goal instanceof AltoGoal.Flee f) {
            return cGoalRunAway.newInstance(f.distance(), f.from().toArray(new BlockPos[0]));
        }
        if (goal instanceof AltoGoal.NearLive nl) {
            BlockPos p = nl.pos().get();
            return p == null ? null : cGoalNear.newInstance(p, nl.range());
        }
        // FleeLive, NearestSatisfying and anything not named above: answer Baritone's two questions
        // through altoclef's own arrival test.
        return proxyGoal(goal);
    }

    /** A Baritone {@code Goal} backed by an {@link AltoGoal}, so no goal shape is left unserved. */
    private static Object proxyGoal(AltoGoal goal) {
        return predicateGoal(goal::reached, goal::target, "altoclef:" + goal);
    }

    /** A Baritone {@code Goal} answered by a raw predicate — for callers that have no AltoGoal
     *  (tungsten's own {@code startNearest} hand-offs). */
    private static Object predicateBaritoneGoal(java.util.function.Predicate<BlockPos> reached) {
        return predicateGoal(reached, () -> null, "altoclef:predicate");
    }

    private static Object predicateGoal(java.util.function.Predicate<BlockPos> reached,
                                        java.util.function.Supplier<Vec3> target, String name) {
        InvocationHandler handler = (proxy, method, args) -> {
            String methodName = method.getName();
            // ⛔ NEVER RETURN NULL FROM A PRIMITIVE-RETURNING METHOD. Baritone's Goal has FIVE
            // methods — isInGoal(int,int,int) and heuristic(int,int,int) on top of the three
            // DEFAULTS isInGoal(BlockPos), heuristic(BlockPos) and heuristic(). A Proxy intercepts
            // the defaults too, so answering only the 3-argument forms made `isInGoal(BlockPos)`
            // return null, and the caller unboxed it to boolean and crashed the client
            // ("Cannot invoke java.lang.Boolean.booleanValue() ... isInGoal", baritone.i.a from
            // PathingBehavior.onTick). Handle every overload, and fall through to a default of the
            // METHOD'S OWN primitive type rather than null.
            if ("isInGoal".equals(methodName)) {
                if (args != null && args.length == 3) {
                    return reached.test(new BlockPos(num(args[0]), num(args[1]), num(args[2])));
                }
                if (args != null && args.length == 1 && args[0] instanceof BlockPos p) {
                    return reached.test(p);
                }
                return false;
            }
            if ("heuristic".equals(methodName)) {
                if (args != null && args.length == 3) {
                    return heuristic(target, num(args[0]), num(args[1]), num(args[2]));
                }
                if (args != null && args.length == 1 && args[0] instanceof BlockPos p) {
                    return heuristic(target, p.getX(), p.getY(), p.getZ());
                }
                return 0.0;
            }
            if ("toString".equals(methodName)) return name;
            if ("hashCode".equals(methodName)) return System.identityHashCode(proxy);
            if ("equals".equals(methodName)) return proxy == args[0];
            Class<?> rt = method.getReturnType();
            if (rt == boolean.class) return false;
            if (rt == double.class) return 0.0;
            if (rt == float.class) return 0f;
            if (rt == int.class) return 0;
            if (rt == long.class) return 0L;
            if (rt == short.class) return (short) 0;
            if (rt == byte.class) return (byte) 0;
            if (rt == char.class) return (char) 0;
            return null;
        };
        return Proxy.newProxyInstance(goalIface.getClassLoader(), new Class<?>[]{goalIface}, handler);
    }

    /** Straight-line distance to the goal's target, or 0 when it has no point to steer at. */
    private static double heuristic(java.util.function.Supplier<Vec3> target, int x, int y, int z) {
        Vec3 t = target == null ? null : target.get();
        if (t == null || Double.isNaN(t.x) || Double.isNaN(t.z)) return 0.0;
        double dx = x + 0.5 - t.x;
        double dy = y - t.y;
        double dz = z + 0.5 - t.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static int num(Object o) {
        return ((Number) o).intValue();
    }

    // ────────────────────── Baritone's own settings, set for us ──────────────────────

    /** The level these settings were applied for; a new world means a new Baritone instance. */
    private static Object settingsAppliedForLevel = null;
    /** Worlds the settings were applied in, and the last values pushed. */
    public static volatile int settingsApplied = 0;
    /**
     * Settings this Baritone build refused or does not have. Read as {@code baritoneSettingsFailed}:
     * <b>0 means every optimal default is genuinely in force</b>, and any non-zero value is named in
     * the chat line on world join. Without it, "the defaults are applied" is an assumption.
     */
    public static volatile int baritoneSettingsFailed = 0;
    private static final java.util.List<String> failedSettings = new java.util.ArrayList<>();
    public static volatile String settingsSummary = "-";

    /**
     * Switch on Baritone's OWN humanisation, which ships with every feature asked for here and had
     * none of it enabled.
     *
     * <p><b>{@code freeLook = false}</b> is the important one. Baritone in free-look mode walks the
     * body without turning the head — so it moves sideways while staring straight ahead ("it doesn't
     * look in the direction it's going"), and when it right-clicks a door it does so <b>without
     * facing it</b>, which is exactly "the bot can't open doors". With free look off, Baritone turns
     * the player to whatever it is walking toward and clicks look where it is aiming.
     *
     * <p><b>{@code smoothLook}</b> eases Baritone's rotation over {@code smoothLookTicks} instead of
     * applying it in one step ("it snaps"). Both are Baritone's own code, so the humanisation is the
     * engine's rather than a second writer fighting it — which is what makes the head stop jittering.
     *
     * <p>Applied through the API, not through {@code #set} chat lines, so it cannot be mistyped and
     * cannot take effect a tick late.
     */
    public static void applyOptimalSettings() {
        if (!ok) return;
        try {
            Object settings = mGetSettings.invoke(null);
            if (settings == null) return;
            StringBuilder done = new StringBuilder();
            // Face where you walk, and click where you look. THE fix for sideways walking and for
            // right-clicking doors without turning to them.
            setSetting(settings, "freeLook", Boolean.FALSE, done);
            // Ease turns instead of snapping. Baritone's own smoother, so there is one writer.
            setSetting(settings, "smoothLook", Boolean.TRUE, done);
            setSetting(settings, "smoothLookTicks", 6, done);
            // Ordinary movement quality; these are Baritone's defaults, asserted so a stray `#set`
            // cannot quietly leave the bot walking everywhere.
            setSetting(settings, "allowSprint", Boolean.TRUE, done);
            setSetting(settings, "allowParkour", Boolean.TRUE, done);
            setSetting(settings, "blockReachDistance", 4.5f, done);
            // ⛔ BARITONE DIGS A HOLE IN FRONT OF THE BED INSTEAD OF WALKING ROUND IT.
            //
            // `blockBreakAdditionalPenalty` is the cost the planner adds for breaking a block, and
            // Baritone ships it at 2.0 — so a single block in the way is cheaper than any detour
            // longer than a couple of steps, and the bot tunnels through whatever is in front of it.
            // Reported by the operator as "@get bed would rather break a block in front of it than
            // walk to the bed".
            //
            // This is the SAME lever this project already pulled on tungsten (breakCostMultiplier
            // 1 -> 4, placeCostMultiplier 1 -> 6) for the same reason: prefer going round over going
            // through. Raised to 8.0, which still lets Baritone dig when digging is the ONLY way
            // (a walled-in bot must get out) — it only stops it digging to save a few steps.
            //
            // Altoclef's own mining is unaffected: ore is reached in ALTOCLEF's reach and mined by
            // ALTOCLEF's tasks. This penalty governs only what the PATHFINDER may chew through.
            setSetting(settings, "blockBreakAdditionalPenalty", 8.0, done);
            setSetting(settings, "blockPlacementPenalty", 20.0, done);
            // ALTOCLEF OWNS DROPS. Baritone scanning them makes it wander after items mid-route,
            // duplicating -- and competing with -- PickupDroppedItemTask. This is what this mod's
            // legacy baritone config meant by "Don't let baritone scan dropped items, we handle
            // that ourselves."
            setSetting(settings, "mineScanDroppedItems", Boolean.FALSE, done);
            // Asserted rather than changed: a stray `#set allowBreak false` would leave the bot
            // unable to path out of any enclosed space, and the two penalties above only make
            // digging EXPENSIVE, never impossible.
            setSetting(settings, "allowBreak", Boolean.TRUE, done);
            setSetting(settings, "allowPlace", Boolean.TRUE, done);
            // ⛔ THIS MOD'S WHOLE PREMISE IS MOVEMENT THAT READS AS HUMAN. Baritone ships with its
            // own anti-cheat mode OFF; switching it on makes its planner respect the constraints a
            // server actually watches for (no impossible turns, no travel the client could not have
            // produced). It costs a little speed and buys the thing the project is FOR.
            setSetting(settings, "antiCheatCompatibility", Boolean.TRUE, done);
            // THE BASELINE, ASSERTED RATHER THAN CHANGED. Every one of these is Baritone's own
            // default and every one is a pure movement-quality win; stating them here means a stray
            // `#set` from a script cannot quietly leave the bot walking everywhere, refusing to
            // climb, or diving into water it will not sprint out of. They are deliberately NOT
            // re-tuned: Baritone's planner values are the author's, measured against a userbase
            // this project does not have.
            setSetting(settings, "sprintAscends", Boolean.TRUE, done);
            setSetting(settings, "sprintInWater", Boolean.TRUE, done);
            setSetting(settings, "allowDiagonalAscend", Boolean.TRUE, done);
            setSetting(settings, "allowDiagonalDescend", Boolean.TRUE, done);
            setSetting(settings, "allowDownward", Boolean.TRUE, done);
            setSetting(settings, "avoidance", Boolean.TRUE, done);
            // Planning performance: cache chunk data instead of re-reading it per search.
            setSetting(settings, "chunkCaching", Boolean.TRUE, done);
            settingsSummary = done.toString();
            settingsApplied++;
            // SAY WHAT DID NOT LAND. A silent refusal is indistinguishable from success, which is the
            // one thing you cannot have when the whole point is "the optimal defaults are applied".
            if (baritoneSettingsFailed > 0) {
                String failed;
                synchronized (failedSettings) {
                    failed = String.join(", ", failedSettings);
                }
                Debug.logUserMessage("Baritone settings applied: " + settingsSummary
                        + "   ** " + baritoneSettingsFailed + " NOT applied: " + failed + " **");
            } else {
                Debug.logUserMessage("Baritone settings applied: " + settingsSummary);
            }
        } catch (Throwable t) {
            bPathFails++;
        }
    }

    private static void setSetting(Object settings, String name, Object value, StringBuilder done) {
        try {
            Object setting = Class.forName("baritone.api.Settings").getField(name).get(settings);
            fValue.set(setting, value);
            if (done.length() > 0) done.append(' ');
            done.append(name).append('=').append(value);
        } catch (Throwable ignored) {
            // ⛔ COUNT IT, DO NOT SWALLOW IT. "Are our defaults actually in force?" is answered by
            // this number and nothing else: a name Baritone does not have (or a value of the wrong
            // boxed type) fails silently here, and the log line below would otherwise claim success.
            baritoneSettingsFailed++;
            synchronized (failedSettings) {
                if (failedSettings.size() < 8) failedSettings.add(name);
            }
        }
    }

    /**
     * The CURRENT value of a Baritone setting, as text ("" when unavailable).
     *
     * <p>The Pathfinder screen fills its fields from this instead of showing a blank box, so the
     * number you are about to change is the number that is actually in force.
     */
    public static String settingValue(String name) {
        if (!ok) return "";
        try {
            Object settings = mGetSettings.invoke(null);
            if (settings == null) return "";
            Object setting = Class.forName("baritone.api.Settings").getField(name).get(settings);
            Object value = fValue.get(setting);
            return value == null ? "" : String.valueOf(value);
        } catch (Throwable t) {
            return "";
        }
    }

    /** Called once per client tick; applies the settings on the first tick of each world. */
    public static void tick() {
        // ⛔ APPLY WHENEVER BARITONE IS PRESENT, not only when it is the selected engine.
        //
        // Baritone is a hard dependency now (see fabric.mod.json), and the settings only matter when
        // Baritone drives — so gating on `selected()` meant a player who had switched to the bundled
        // engine never received them, and switching back mid-game would drive on defaults. Applying
        // them regardless costs one reflection pass per world and removes that trap.
        if (!available()) return;
        Object level = net.minecraft.client.Minecraft.getInstance().level;
        if (level == null) return;
        if (level == settingsAppliedForLevel) return;
        settingsAppliedForLevel = level;
        applyOptimalSettings();
    }

    // ────────────────────────── tungsten's movement delegate ──────────────────────────

    /**
     * Ask Baritone to COLLECT dropped items matching {@code which}.
     *
     * <p>{@code IFollowProcess.pickup} is Baritone's own "go and get that dropped item": it paths to
     * the drop and collects it, using the same movement as everything else — which means <b>it opens
     * doors on the way</b>. That is the whole point of this hook. Altoclef's own pickup drives the
     * body itself, and reported by the operator it could not get through a door, so a drop behind one
     * meant {@code @get} never completed: the gather was finished, the item was lying there, and the
     * bot could not walk through the doorway to reach it.
     *
     * @return true when the request was issued
     */
    public static boolean pickupItems(java.util.function.Predicate<net.minecraft.world.item.ItemStack> which) {
        Object bar = baritone();
        if (bar == null || which == null) return false;
        try {
            mPickup.invoke(mGetFollow.invoke(bar), which);
            return true;
        } catch (Throwable t) {
            bPathFails++;
            return false;
        }
    }

    /** Is Baritone's follow process running — following a player, or collecting drops? */
    public static boolean isPickingUp() {
        Object bar = baritone();
        if (bar == null) return false;
        try {
            return Boolean.TRUE.equals(mProcessActive.invoke(mGetFollow.invoke(bar)));
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Ask Baritone to MINE a quantity of these blocks.
     *
     * <p>Baritone's own mining gets the block, and it does the part altoclef kept getting wrong: it
     * chooses a target it can actually reach, walks to a position from which it can be seen, and
     * mines <b>that</b> block — instead of swinging at whatever the cursor happens to land on.
     * Reported by the operator as "it will break a block blocking the cursor from the bed", and the
     * right answer is to let the engine that does not do that mine it.
     *
     * <p>Drops are left for us: {@code mineScanDroppedItems} is set false in
     * {@link #applyOptimalSettings}, so Baritone does not chase them and altoclef's ordinary
     * ResourceTask pickup collects them — one owner for each job.
     *
     * @return true when the request was issued
     */
    public static boolean mineBlocks(int quantity, net.minecraft.world.level.block.Block... blocks) {
        Object bar = baritone();
        if (bar == null || blocks == null || blocks.length == 0) return false;
        try {
            Object mine = mGetMine.invoke(bar);
            // ⛔ WRAP THE ARGUMENTS. `invoke(Object, Object...)` treats a trailing Object[] AS the
            // varargs array, and a Block[] is one — so `invoke(mine, quantity, blocks)` would pass a
            // single argument and throw. The explicit Object[] is what makes this two arguments.
            mMineBlocks.invoke(mine, new Object[]{quantity, blocks});
            return true;
        } catch (Throwable t) {
            bPathFails++;
            return false;
        }
    }

    /** Is Baritone's mine process running? */
    public static boolean isMining() {
        Object bar = baritone();
        if (bar == null) return false;
        try {
            return Boolean.TRUE.equals(mProcessActive.invoke(mGetMine.invoke(bar)));
        } catch (Throwable t) {
            return false;
        }
    }

    /** Stop Baritone's mine process. */
    public static void cancelMining() {
        Object bar = baritone();
        if (bar == null) return;
        try {
            mCancelProcess.invoke(mGetMine.invoke(bar));
        } catch (Throwable ignored) {
            // a stop that fails is not worth killing a tick over
        }
    }

    /**
     * Release Baritone so the game can actually EXIT.
     *
     * <h2>The bug this fixes</h2>
     *
     * Every session ended with a crash report — <b>fourteen</b> on this machine, all reading
     * {@code Description: Client shutdown from post-main / java.lang.Error: Watchdog}. That is not a
     * crash in game: it is the client's SHUTDOWN watchdog. The window closes, the JVM refuses to
     * exit, and the watchdog fires and writes a crash report.
     *
     * <p>A thread dump from the newest one names the reason — the threads still alive are the ones
     * <b>not</b> marked {@code daemon}:
     *
     * <pre>
     *   "Thread-1"            py4j.GatewayServer.run     (blocked in ServerSocket.accept)
     *   "pool-6-thread-1..4"  baritone.o$a.run           (Baritone's thread pools)
     * </pre>
     *
     * <p>A JVM does not exit while a non-daemon thread lives. Baritone's pools never stop on their
     * own and nothing ever called {@code destroyBaritone} — so the process sat holding memory after
     * every quit, which on an 8 GB machine is a real cost and not merely a noisy file.
     *
     * <p>{@code IBaritoneProvider.destroyBaritone} is the documented way to stop one. It is called
     * from the client-stopping hook in {@code AltoclefClient}, beside the py4j shutdown that until
     * now only the dev harness ever invoked.
     */
    public static void shutdown() {
        Object bar = baritone();
        if (bar == null) return;
        try {
            Object provider = mGetProvider.invoke(null);
            if (provider != null) {
                mDestroy.invoke(provider, bar);
                Debug.logInternal("Baritone shut down (thread pools released; the JVM can exit).");
            }
        } catch (Throwable ignored) {
            // a shutdown that complains is worse than one that does nothing
        }
    }

    /**
     * Send the body to a world point (a Vec3 target, floored to a cell, arrived within 1 block).
     */
    public static boolean pathToPoint(double x, double y, double z) {
        try {
            return sendGoal(cGoalNear.newInstance(
                    new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z)), 1));
        } catch (Throwable t) {
            bPathFails++;
            return false;
        }
    }

    /** Send the body INTO one exact cell — Baritone's GoalBlock, which digs if it has to. That is
     *  what the builder positioning itself and the pillar both want. */
    public static boolean pathToExact(int x, int y, int z) {
        try {
            return sendGoal(cGoalBlock.newInstance(x, y, z));
        } catch (Throwable t) {
            bPathFails++;
            return false;
        }
    }

    /** Send the body to the nearest cell satisfying {@code reached}. */
    public static boolean pathToNearest(java.util.function.Predicate<BlockPos> reached) {
        return sendGoal(predicateBaritoneGoal(reached));
    }

    /** Set the goal and start walking it — the two calls Baritone's own {@code setGoalAndPath} makes. */
    private static boolean sendGoal(Object baritoneGoal) {
        if (baritoneGoal == null) return false;
        Object bar = baritone();
        if (bar == null) return false;
        try {
            Object cgp = mGetCustomGoal.invoke(bar);
            mSetGoal.invoke(cgp, baritoneGoal);
            mPath.invoke(cgp);
            bPathRequests++;
            return true;
        } catch (Throwable t) {
            bPathFails++;
            return false;
        }
    }

    /**
     * The object tungsten sees through {@link kaptainwutax.tungsten.api.MovementDelegates}.
     *
     * <p>An adapter rather than the class itself: the interface's instance {@code selected()} cannot
     * coexist with the static {@link #selected()} the rest of the mod calls.
     */
    private static final class Delegate implements kaptainwutax.tungsten.api.ExternalMovement {
        @Override public boolean selected() { return BaritoneBridge.selected(); }
        @Override public boolean active() { return BaritoneBridge.isPathing(); }
        @Override public boolean pathTo(double x, double y, double z) { return BaritoneBridge.pathToPoint(x, y, z); }
        @Override public boolean pathExact(int x, int y, int z) { return BaritoneBridge.pathToExact(x, y, z); }
        @Override public boolean pathNearest(java.util.function.Predicate<BlockPos> reached) {
            return BaritoneBridge.pathToNearest(reached);
        }
        @Override public boolean pillarTo(int y, int x, int z) {
            // PillarTask's 1- and 2-arg forms pass MIN_VALUE for "the column I am standing in".
            int colX = x, colZ = z;
            if (colX == Integer.MIN_VALUE || colZ == Integer.MIN_VALUE) {
                var p = net.minecraft.client.Minecraft.getInstance().player;
                if (p == null) return false;
                colX = p.blockPosition().getX();
                colZ = p.blockPosition().getZ();
            }
            return BaritoneBridge.pathToExact(colX, y, colZ);
        }
        @Override public void stop() { BaritoneBridge.cancel(); }
    }
}

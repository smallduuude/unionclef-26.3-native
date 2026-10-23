package adris.altoclef.tasks.misc;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasks.movement.DefaultGoToDimensionTask;
import adris.altoclef.tasks.movement.GetToEntityTask;
import adris.altoclef.tasks.movement.GetToXZTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.StructureLocator;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.time.TimerGame;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Generic "obtain N of item X from a villager of profession P" task: the survival source of the
 * trade-only items, above all the 26.3 cartographer structure maps.
 *
 * <h2>How the trade is executed</h2>
 * It does exactly what a player does, through the vanilla client:
 * <ol>
 *   <li>locate a village with {@link StructureLocator} (cheat-free; singleplayer only), travel there
 *       with {@link GetToXZTask} and find the wanted villager with {@code EntityTracker};</li>
 *   <li>walk within interaction reach and right-click the villager, which opens the
 *       {@link MerchantMenu} (the client-owned container menu; no GUI ownership hack);</li>
 *   <li>read the offers from the menu ({@code MerchantMenu.getOffers()}), pick the one whose result
 *       is the wanted item, and select it with a {@link ServerboundSelectTradePacket} -- the same
 *       packet {@code MerchantScreen.postButtonClick()} sends; the server then moves the payment
 *       from the player's inventory into the two payment slots ({@code MerchantMenu.tryMoveItems});</li>
 *   <li>click the result slot with a normal PICKUP container click: vanilla's
 *       {@code MerchantResultSlot.onTake} executes the trade (consumes payment, awards xp, increments
 *       uses) and the result lands in the cursor, which is then placed into the inventory;</li>
 *   <li>close the screen and re-open it for the next trade, so offers and villager level are always
 *       the server's own freshly synced copy.</li>
 * </ol>
 *
 * <h2>Leveling</h2>
 * 26.3 trade data ({@code data/minecraft/village_trade/<profession>/<level>/}): cartographer village
 * maps are level-2 trades, trial-chamber/monument maps level 3, woodland-mansion map and globe banner
 * pattern level 5. While the wanted offer is absent the task performs the best "sell" offer available
 * (result = emerald, so it also produces the emeralds the map purchase needs), closes and re-opens the
 * screen, and repeats until the villager reaches the level the wanted trade lives at. The level itself
 * is read from the server-synced menu ({@code MerchantMenu.getTraderLevel()}) and villager data, so no
 * xp arithmetic is guessed; the grind is hard-capped ({@link #MAX_TRADES}).
 *
 * <p><b>Cost of leveling</b> (a real limitation, measured from the 26.3 trade data): a cartographer
 * sells paper (24 -&gt; 1 emerald, +2 xp) at level 1, glass panes (11 -&gt; 1 emerald, +10 xp) at level
 * 2 and a compass (1 -&gt; 1 emerald, +20 xp) at level 3. Reaching level 5 therefore needs about 120
 * paper, 66 glass panes and 9 compasses (36 iron + 9 redstone) in leveling trades alone, plus the map
 * price (8-14 emeralds and a compass). That is a long resource operation; it is bounded and honest,
 * not skipped. The emeralds earned by those sells usually cover the map price.
 *
 * <h2>Villager type matters in 26.3</h2>
 * Every cartographer map trade carries a {@code merchant_predicate} on the merchant's villager variant
 * (e.g. the snowy-village map is only sold by taiga/swamp villagers, the desert-village map only by
 * savanna/jungle ones), and each level rolls only {@code amount: 2} offers out of its trade set. The
 * caller passes the allowed types and the matching village structure ids: a wrong-type villager is
 * skipped (leveling it would only waste paper) and, if a correctly-typed cartographer of the required
 * level still does not offer the wanted map, this task abandons that villager, visits another village
 * and says so in the log. It never reports success it did not observe.
 *
 * <h2>Fallbacks</h2>
 * When the village has no villager of the wanted profession, the task finds an unemployed villager,
 * places the profession's job site block (cartography table / barrel) next to it and waits for the
 * villager AI to claim the job, then re-scans. If that fails too, it moves to the next village.
 *
 * <p>On a multiplayer server {@link StructureLocator} is unavailable, so the owning chain does not
 * offer this task at all.
 */
public class TradeWithVillagerTask extends ResourceTask {

    /** Locator rings in chunks, as used by vanilla {@code /locate}. */
    private static final int[] LOCATE_RINGS = {100, 200, 400};
    /** Villages to visit before giving up. One attempt can cost a lot of paper to level a cartographer. */
    private static final int MAX_VILLAGES = 3;
    /** Hard cap on completed trades for one invocation (reaching level 5 needs about 20 leveling trades). */
    private static final int MAX_TRADES = 80;
    /** Horizontal distance from the locate anchor that counts as "arrived in the village". */
    private static final double ARRIVE_RADIUS = 24.0;
    /** How far to walk away between villages, so the locator returns a different one. */
    private static final int RETREAT_DISTANCE = 512;
    /** Interaction reach for a villager (the server checks about 3 blocks). */
    private static final double INTERACT_RANGE = 2.8;
    /** How long to wait for the trade screen after a right-click before retrying. */
    private static final double OPEN_WAIT_SECONDS = 2.0;
    /** Right-click attempts before abandoning this villager as untradeable. */
    private static final int MAX_OPEN_ATTEMPTS = 8;
    /** Delay between the container clicks of one trade (the slot handler rate limit is 0.2 s). */
    private static final double CLICK_STEP_SECONDS = 0.35;
    /** Quiet wait after a trade before re-opening the screen (server clears the trading player). */
    private static final double POST_TRADE_SECONDS = 0.5;
    /** Wait for the villager AI to claim a job site we placed before calling the fallback failed. */
    private static final double JOB_CLAIM_SECONDS = 14.0;
    /** Ticks to wait for a lost villager to load back in at its village before moving on. */
    private static final int LOST_VILLAGER_WAIT_TICKS = 240;
    /** Ticks to wait for villagers to load in after arriving at a village before looking elsewhere. */
    private static final int FIND_VILLAGER_WAIT_TICKS = 160;
    /** Guard against a pathological "open screen and change nothing" loop. */
    private static final int MAX_SCREEN_CYCLES = 300;

    private enum Phase {LOCATE, TRAVEL, FIND, EMPLOY, TRADE, NEXT, DONE}

    /** Which container click of the one-trade sequence is in flight. */
    private enum MenuAction {NONE, SELECT, WAIT_RESULT, WAIT_PICKUP, WAIT_STORE}

    /** A payment requirement read from the actual offer (never hardcoded: the server's data wins). */
    private record Cost(Item item, int count) {
    }

    /** A chosen trade for the currently open screen. */
    private record TradePlan(int offerIndex, boolean buyWanted, List<Cost> costs, String label) {
    }

    private final Item wantedItem;
    private final int wantedCount;
    private final ResourceKey<VillagerProfession> profession;
    private final int requiredLevel;
    private final Set<ResourceKey<VillagerType>> allowedVillagerTypes;
    private final List<String> villageStructureIds;
    private final Item jobSiteItem;
    private final Block jobSiteBlock;

    private Phase phase = Phase.LOCATE;
    private CompletableFuture<StructureLocator.Located> locating;
    private StructureLocator.Located site;
    private int villagesTried = 0;
    private int tradesDone = 0;
    private int screenCycles = 0;
    private boolean gaveUp = false;
    private String giveUpReason = "";

    /** The villager being traded with; remembered by UUID so a chunk reload keeps identity. */
    private UUID targetVillagerId;
    private Villager targetVillager;
    private BlockPos targetHomePos;

    /** Villagers that cannot sell the wanted item (wrong random roll) or cannot be traded with. */
    private final Set<UUID> hopelessVillagers = new HashSet<>();
    /** Villagers already used for the job-site fallback in this village. */
    private final Set<UUID> employmentTried = new HashSet<>();

    /** A catalogue task we are waiting to finish (payment materials / job site block). */
    private Task pendingGatherTask;
    /** The job-site placement child, while it runs. */
    private PlaceBlockNearbyTask placeJobSiteTask;
    private Villager employmentTarget;
    private boolean employmentAttempted = false;
    /** True once we have placed a job-site block in this village (the scanner may lag behind). */
    private boolean jobSitePlacedByUs = false;
    /** True once a placement attempt failed for this village, so we do not retry it forever. */
    private boolean placementTried = false;

    private final TimerGame stepTimer = new TimerGame(CLICK_STEP_SECONDS);
    private final TimerGame openAttemptTimer = new TimerGame(OPEN_WAIT_SECONDS);
    private final TimerGame postTradeTimer = new TimerGame(POST_TRADE_SECONDS);
    private final TimerGame jobClaimTimer = new TimerGame(JOB_CLAIM_SECONDS);
    private final TimerGame placeAttemptTimer = new TimerGame(60.0);
    private final TimerGame locateTimer = new TimerGame(40.0);
    private final TimerGame offersWaitTimer = new TimerGame(3.0);
    /** Materials gathered for one payment; a guard against a catalogue task that cannot deliver. */
    private int gatherCycles = 0;

    private MenuAction menuAction = MenuAction.NONE;
    private int pendingOfferIndex = -1;
    private boolean pendingBuyWanted = false;
    private String pendingLabel = "";
    private int selectRetries = 0;
    private int pickupRetries = 0;
    private int storeRetries = 0;
    private int openAttempts = 0;
    private List<Cost> pendingCosts = List.of();
    private int tradeScreenResultSlot = -1;
    /** What the in-flight trade must deliver, and what we held before it (post-trade verification). */
    private Item expectedTradeItem;
    private int expectedTradeDelta;
    private int expectedTradeCountBefore;

    private int findVillagerWaitTicks = 0;
    private int lostVillagerWaitTicks = 0;
    private int failedMenuCycles = 0;
    /** A movement task the villager resolver wants the trade phase to run instead of trading. */
    private Task scheduledMovement;

    public TradeWithVillagerTask(Item item, int count, ResourceKey<VillagerProfession> profession,
                                 int requiredLevel, Set<ResourceKey<VillagerType>> allowedVillagerTypes,
                                 List<String> villageStructureIds, Item jobSiteItem, Block jobSiteBlock) {
        super(new ItemTarget(item, count));
        this.wantedItem = item;
        this.wantedCount = count;
        this.profession = profession;
        this.requiredLevel = Math.max(1, requiredLevel);
        this.allowedVillagerTypes = allowedVillagerTypes == null ? Set.of() : Set.copyOf(allowedVillagerTypes);
        this.villageStructureIds = villageStructureIds == null ? List.of() : List.copyOf(villageStructureIds);
        this.jobSiteItem = jobSiteItem;
        this.jobSiteBlock = jobSiteBlock;
    }

    // ───────────────────────────────── resource task plumbing ─────────────────────────────────

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        Debug.logMessage("Trade: want " + wantedCount + "x " + displayName(wantedItem)
                + " from a " + professionName() + " (level " + requiredLevel + ")");
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        closeTradeScreen(mod);
        if (gaveUp) {
            Debug.logMessage("Trade: stopped after giving up (" + giveUpReason + ")");
        }
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (!(other instanceof TradeWithVillagerTask task)) return false;
        return task.wantedItem == wantedItem
                && task.wantedCount == wantedCount
                && task.profession.equals(profession)
                && task.requiredLevel == requiredLevel;
    }

    @Override
    protected String toDebugStringName() {
        return "Trading " + displayName(wantedItem) + " from " + professionName();
    }

    @Override
    public boolean isFinished() {
        return gaveUp || super.isFinished();
    }

    // ────────────────────────────────────── main state machine ──────────────────────────────────

    @Override
    protected Task onResourceTick(AltoClef mod) {
        if (gaveUp) return null;
        if (super.isFinished()) {
            finishSuccessfully(mod);
            return null;
        }
        if (tradesDone >= MAX_TRADES) {
            giveUp(mod, "reached the " + MAX_TRADES + " trade cap without the item");
            return null;
        }
        if (mod.getWorld() != null && !mod.getWorld().dimension().equals(Level.OVERWORLD)) {
            setDebugState("Going to the overworld for villages");
            return new DefaultGoToDimensionTask(Dimension.OVERWORLD);
        }

        // A catalogue gather task keeps running until it reports the materials are in the inventory.
        if (pendingGatherTask != null) {
            if (!pendingGatherTask.isFinished()) {
                return pendingGatherTask;
            }
            pendingGatherTask = null;
            return null;
        }

        switch (phase) {
            case LOCATE -> {
                return tickLocate(mod);
            }
            case TRAVEL -> {
                return tickTravel(mod);
            }
            case FIND -> {
                return tickFind(mod);
            }
            case EMPLOY -> {
                return tickEmploy(mod);
            }
            case TRADE -> {
                return tickTrade(mod);
            }
            case NEXT -> {
                return tickNext(mod);
            }
            case DONE -> {
                return null;
            }
        }
        return null;
    }

    private Task tickLocate(AltoClef mod) {
        if (!StructureLocator.isAvailable()) {
            giveUp(mod, "no integrated server: villages cannot be located cheat-free in multiplayer");
            return null;
        }
        if (locating == null) {
            if (villagesTried >= MAX_VILLAGES) {
                giveUp(mod, "visited " + villagesTried + " villages without obtaining " + displayName(wantedItem));
                return null;
            }
            villagesTried++;
            setDebugState("Locating village (attempt " + villagesTried + "/" + MAX_VILLAGES + ")");
            Debug.logMessage("Trade: locating a village for " + displayName(wantedItem)
                    + " (attempt " + villagesTried + "/" + MAX_VILLAGES + ")");
            locating = StructureLocator.locateWithRings(mod, villageStructureIds, LOCATE_RINGS);
            locateTimer.reset();
            return null;
        }
        if (!locating.isDone()) {
            if (locateTimer.elapsed()) {
                locating = null;
                giveUp(mod, "the village locator did not answer within 40 seconds");
                return null;
            }
            setDebugState("Locating village ...");
            return null;
        }
        StructureLocator.Located found;
        try {
            found = locating.getNow(null);
        } catch (Throwable t) {
            found = null;
        }
        locating = null;
        if (found == null) {
            giveUp(mod, "no village found within the locate rings");
            return null;
        }
        site = found;
        targetVillager = null;
        targetVillagerId = null;
        targetHomePos = null;
        employmentTarget = null;
        employmentAttempted = false;
        jobSitePlacedByUs = false;
        placementTried = false;
        findVillagerWaitTicks = 0;
        lostVillagerWaitTicks = 0;
        gatherCycles = 0;
        setDebugState("Found " + site);
        Debug.logMessage("Trade: found " + found);
        phase = Phase.TRAVEL;
        return null;
    }

    private Task tickTravel(AltoClef mod) {
        if (site == null) {
            phase = Phase.LOCATE;
            return null;
        }
        if (horizontalDistance(mod, site.pos()) <= ARRIVE_RADIUS) {
            Debug.logMessage("Trade: entered the village at " + site.pos().toShortString());
            setDebugState("Entered village");
            phase = Phase.FIND;
            findVillagerWaitTicks = 0;
            return null;
        }
        setDebugState("Travelling to village " + site.pos().toShortString());
        return new GetToXZTask(site.pos().getX(), site.pos().getZ(), Dimension.OVERWORLD);
    }

    private Task tickFind(AltoClef mod) {
        Villager candidate = findEligibleVillager(mod);
        if (candidate != null) {
            adoptVillager(candidate);
            Debug.logMessage("Trade: found " + professionName() + " villager at "
                    + candidate.blockPosition().toShortString()
                    + " (level " + villagerLevel(candidate) + ")");
            setDebugState("Found " + professionName());
            phase = Phase.TRADE;
            return null;
        }
        // Villagers may still be loading in; give the chunk a moment before assuming there is none.
        findVillagerWaitTicks++;
        // Only try to create the job after the tracker has had a chance to see the village's own
        // villagers; otherwise a cartographer that is still loading would be missed and we would
        // spend a cartography table on an unemployed villager for nothing.
        if (!employmentAttempted && findVillagerWaitTicks > 40
                && jobSiteItem != null && jobSiteBlock != null) {
            Villager jobless = findJoblessVillager(mod);
            if (jobless != null) {
                employmentTarget = jobless;
                phase = Phase.EMPLOY;
                setDebugState("Unemployed villager found; will place a job site");
                return null;
            }
        }
        if (findVillagerWaitTicks > FIND_VILLAGER_WAIT_TICKS) {
            Debug.logWarning("Trade: no " + professionName() + " in the village at " + site + ", moving on");
            phase = Phase.NEXT;
            return null;
        }
        setDebugState("Searching the village for a " + professionName() + " ...");
        if (findVillagerWaitTicks % 40 == 1) {
            return new TimeoutWanderTask(2, true);
        }
        return null;
    }

    /** Job-site fallback: place the profession block next to an unemployed villager and wait. */
    private Task tickEmploy(AltoClef mod) {
        Villager candidate = findEligibleVillager(mod);
        if (candidate != null) {
            adoptVillager(candidate);
            Debug.logMessage("Trade: villager claimed the job site and is now a " + professionName());
            setDebugState("Villager took the job");
            phase = Phase.TRADE;
            return null;
        }
        if (placeJobSiteTask != null) {
            if (!placeJobSiteTask.isFinished()) {
                if (placeAttemptTimer.elapsed()) {
                    Debug.logWarning("Trade: could not place the " + displayName(jobSiteItem)
                            + " job site in time; giving up on this village");
                    placeJobSiteTask = null;
                    phase = Phase.NEXT;
                    return null;
                }
                setDebugState("Placing a " + displayName(jobSiteItem) + " near the villager");
                return placeJobSiteTask;
            }
            placeJobSiteTask = null;
            jobSitePlacedByUs = true;
            jobClaimTimer.reset();
            return null;
        }
        Villager jobless = employmentTarget;
        if (jobless == null || !jobless.isAlive()) {
            jobless = findJoblessVillager(mod);
            employmentTarget = jobless;
        }
        if (jobless == null) {
            employmentAttempted = true;
            Debug.logWarning("Trade: no unemployed villager left for the job-site fallback");
            phase = Phase.NEXT;
            return null;
        }
        boolean siteExists = jobSitePlacedByUs || hasJobSiteNear(mod, jobless);
        if (siteExists) {
            // A job site exists; only the AI's claim is missing.
            if (jobClaimTimer.elapsed()) {
                employmentAttempted = true;
                employmentTried.add(jobless.getUUID());
                Debug.logWarning("Trade: the villager did not take the "
                        + displayName(jobSiteItem) + " job site in time");
                phase = Phase.NEXT;
                return null;
            }
            setDebugState("Waiting for the villager to take the job");
            return null;
        }
        if (placementTried) {
            employmentAttempted = true;
            phase = Phase.NEXT;
            return null;
        }
        if (!mod.getItemStorage().hasItem(jobSiteItem)) {
            ResourceTask gather = TaskCatalogue.getItemTask(jobSiteItem, 1);
            if (gather == null) {
                giveUp(mod, "no source for " + displayName(jobSiteItem));
                return null;
            }
            setDebugState("Getting a " + displayName(jobSiteItem));
            pendingGatherTask = gather;
            return gather;
        }
        if (!jobless.closerThan(mod.getPlayer(), 14)) {
            setDebugState("Approaching the unemployed villager");
            return new GetToEntityTask(jobless, 4.0);
        }
        final Villager finalJobless = jobless;
        placeJobSiteTask = new PlaceBlockNearbyTask(
                pos -> pos.closerToCenterThan(finalJobless.position(), 6), jobSiteBlock);
        placementTried = true;
        jobClaimTimer.reset();
        placeAttemptTimer.reset();
        setDebugState("Placing a " + displayName(jobSiteItem) + " near the villager");
        return placeJobSiteTask;
    }

    private Task tickNext(AltoClef mod) {
        closeTradeScreen(mod);
        if (villagesTried >= MAX_VILLAGES) {
            giveUp(mod, "tried " + villagesTried + " villages; no villager would sell " + displayName(wantedItem));
            return null;
        }
        if (site == null) {
            phase = Phase.LOCATE;
            return null;
        }
        BlockPos retreat = site.pos().offset(
                (int) Math.round(Math.cos(Math.toRadians(37 + villagesTried * 113)) * RETREAT_DISTANCE), 0,
                (int) Math.round(Math.sin(Math.toRadians(37 + villagesTried * 113)) * RETREAT_DISTANCE));
        if (mod.getPlayer().blockPosition().distToCenterSqr(retreat.getX(), retreat.getY(), retreat.getZ())
                <= 40 * 40) {
            site = null;
            phase = Phase.LOCATE;
            return null;
        }
        setDebugState("Moving away to find another village");
        return new GetToXZTask(retreat.getX(), retreat.getZ(), Dimension.OVERWORLD);
    }

    // ───────────────────────────────────────── the trade phase ─────────────────────────────────

    private Task tickTrade(AltoClef mod) {
        // 1. The trade screen is open: work inside it.
        MerchantMenu menu = getOpenMerchantMenu(mod);
        if (menu != null) {
            return tickMerchantMenu(mod, menu);
        }
        if (menuAction != MenuAction.NONE) {
            // The screen disappeared under us (interrupted or closed); start over cleanly.
            resetMenuState();
            postTradeTimer.reset();
        }

        // 2. Gather the payment we already decided we need, before walking back to the villager.
        if (!pendingCosts.isEmpty()) {
            List<Cost> missing = missingCosts(mod, pendingCosts);
            if (!missing.isEmpty()) {
                if (++gatherCycles > 100) {
                    giveUp(mod, "the catalogue could not deliver the payment items");
                    return null;
                }
                Cost next = missing.get(0);
                setDebugState("Gathering " + next.count() + "x " + displayName(next.item()));
                ResourceTask gather = TaskCatalogue.getItemTask(next.item(), next.count());
                if (gather == null) {
                    giveUp(mod, "no source for payment item " + displayName(next.item()));
                    return null;
                }
                pendingGatherTask = gather;
                return gather;
            }
            pendingCosts = List.of();
        }

        // 3. Find (or re-find) the villager; this may schedule a walk back to the village.
        Villager villager = resolveTradeVillager(mod);
        if (scheduledMovement != null) {
            Task movement = scheduledMovement;
            scheduledMovement = null;
            return movement;
        }
        if (villager == null) {
            return null;
        }

        // 4. Don't trade with a full cursor or a full inventory: vanilla would leave the result in
        //    the cursor and closing the screen can drop it.
        if (!StorageHelper.getItemStackInCursorSlot().isEmpty()) {
            setDebugState("Clearing the cursor");
            Optional<adris.altoclef.util.slots.Slot> fit =
                    mod.getItemStorage().getSlotThatCanFitInPlayerInventory(
                            StorageHelper.getItemStackInCursorSlot(), false);
            fit.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, ContainerInput.PICKUP));
            return null;
        }
        if (!mod.getItemStorage().hasEmptyInventorySlot()) {
            setDebugState("Making room in the inventory");
            return new EnsureFreeInventorySlotTask();
        }

        // 5. Quiet moment after the previous trade, so the server cleared the trading player.
        if (!postTradeTimer.elapsed()) {
            setDebugState("Traded " + tradesDone + " times");
            return null;
        }

        // 6. Walk into interaction reach (GetToEntityTask has no isFinished(): it is simply
        //    withdrawn once the distance test below passes).
        if (!villager.closerThan(mod.getPlayer(), INTERACT_RANGE)) {
            setDebugState("Approaching " + professionName());
            return new GetToEntityTask(villager, 2.4);
        }

        // 7. Villagers cannot be traded with while asleep.
        if (villager.isSleeping()) {
            setDebugState("Villager is asleep (waiting for daytime)");
            return null;
        }

        // 8. Right-click to open the trade screen. Empty the hand first: a bucket/bow/block in the
        //    hand would be used instead of (or before) the interaction.
        if (!openAttemptTimer.elapsed()) {
            setDebugState("Opening the trade screen");
            return null;
        }
        if (openAttempts >= MAX_OPEN_ATTEMPTS) {
            Debug.logWarning("Trade: could not open the trade screen for " + professionName()
                    + "; blacklisting this villager");
            hopelessVillagers.add(villager.getUUID());
            targetVillager = null;
            targetVillagerId = null;
            targetHomePos = null;
            openAttempts = 0;
            resetMenuState();
            phase = Phase.FIND;
            findVillagerWaitTicks = 0;
            return null;
        }
        mod.getSlotHandler().forceDeequipRightClickableItem();
        openAttempts++;
        openAttemptTimer.reset();
        setDebugState("Right-clicking " + professionName());
        try {
            mod.getController().interact(mod.getPlayer(), villager,
                    new EntityHitResult(villager), InteractionHand.MAIN_HAND);
        } catch (Exception e) {
            Debug.logWarning("Trade: interaction with villager failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Find the villager to trade with. Returns null when it scheduled a movement/meta action instead
     * (the caller then runs {@link #scheduledMovement} or simply idles for a tick).
     */
    private Villager resolveTradeVillager(AltoClef mod) {
        if (targetVillager != null && targetVillager.isAlive()) {
            return targetVillager;
        }
        targetVillager = findRememberedVillager(mod);
        if (targetVillager != null) {
            return targetVillager;
        }
        // The chunk may have unloaded while we were gathering. Walk back to the last known position.
        if (targetVillagerId != null && targetHomePos != null) {
            double dx = mod.getPlayer().getX() - targetHomePos.getX();
            double dz = mod.getPlayer().getZ() - targetHomePos.getZ();
            if (dx * dx + dz * dz > 40 * 40) {
                scheduledMovement = new GetToXZTask(targetHomePos.getX(), targetHomePos.getZ(),
                        Dimension.OVERWORLD);
                setDebugState("Walking back to the villager");
                return null;
            }
            lostVillagerWaitTicks++;
            if (lostVillagerWaitTicks < LOST_VILLAGER_WAIT_TICKS) {
                setDebugState("Waiting for the villager to load back in");
                if (lostVillagerWaitTicks % 60 == 1) {
                    scheduledMovement = new TimeoutWanderTask(2, true);
                }
                return null;
            }
            Debug.logWarning("Trade: lost track of the villager and it did not come back; moving on");
        }
        // Give up on that villager and use any other eligible one still tracked, or re-search.
        targetVillager = null;
        targetVillagerId = null;
        targetHomePos = null;
        lostVillagerWaitTicks = 0;
        Villager candidate = findEligibleVillager(mod);
        if (candidate != null) {
            adoptVillager(candidate);
            return candidate;
        }
        phase = Phase.FIND;
        findVillagerWaitTicks = 0;
        return null;
    }

    // ───────────────────────────────────── the merchant menu ───────────────────────────────────

    private Task tickMerchantMenu(AltoClef mod, MerchantMenu menu) {
        if (tradeScreenResultSlot < 0) {
            tradeScreenResultSlot = resultSlotIndex(menu);
            screenCycles++;
            if (screenCycles > MAX_SCREEN_CYCLES) {
                giveUp(mod, "opened the trade screen " + screenCycles + " times without success");
                return null;
            }
            openAttempts = 0;
            menuAction = MenuAction.NONE;
            selectRetries = 0;
            pickupRetries = 0;
            storeRetries = 0;
            offersWaitTimer.reset();
        }

        if (menuAction != MenuAction.NONE) {
            return tickMenuAction(mod, menu);
        }

        MerchantOffers offers = menu.getOffers();
        if (offers == null || offers.isEmpty()) {
            if (offersWaitTimer.elapsed()) {
                abortMenuCycle(mod, "the trade screen never received any offers");
                return null;
            }
            setDebugState("Waiting for trade offers");
            return null;
        }

        TradePlan plan = decideTradePlan(mod, menu, offers);
        if (plan == null) {
            return null; // decideTradePlan closed the screen or recorded what to gather
        }
        List<Cost> missing = missingCosts(mod, plan.costs());
        if (!missing.isEmpty()) {
            // Close the screen and gather the payment. The next opening re-decides against a fresh,
            // server-synced offer list, so a stale plan is never acted on.
            pendingCosts = plan.costs();
            setDebugState("Need payment for " + plan.label());
            closeTradeScreen(mod);
            return null;
        }
        if (!mod.getItemStorage().hasEmptyInventorySlot()) {
            pendingCosts = List.of();
            closeTradeScreen(mod);
            setDebugState("Making room in the inventory");
            return new EnsureFreeInventorySlotTask();
        }
        pendingOfferIndex = plan.offerIndex();
        pendingBuyWanted = plan.buyWanted();
        pendingLabel = plan.label();
        // What the trade must deliver, checked after the click sequence so a "click went through"
        // is never mistaken for "the trade happened".
        MerchantOffer chosen = offers.get(plan.offerIndex());
        expectedTradeItem = chosen.getResult().getItem();
        expectedTradeDelta = Math.max(1, chosen.getResult().getCount());
        expectedTradeCountBefore = mod.getItemStorage().getItemCountInventoryOnly(expectedTradeItem);
        menuAction = MenuAction.SELECT;
        stepTimer.reset();
        return null;
    }

    /**
     * Read the villager's current offers and choose exactly one trade: buy the wanted item if it is
     * available and affordable, otherwise sell something for emeralds / level the villager up.
     * Returns null (after closing the screen and/or recording payment to gather) when no trade can be
     * started from this screen.
     */
    private TradePlan decideTradePlan(AltoClef mod, MerchantMenu menu, MerchantOffers offers) {
        int wantedIndex = findOfferFor(offers, wantedItem);
        int level = currentVillagerLevel(menu);
        MerchantOffer wantedOffer = wantedIndex >= 0 ? offers.get(wantedIndex) : null;
        boolean wantedAvailable = wantedOffer != null && !wantedOffer.isOutOfStock();
        if (wantedAvailable) {
            List<Cost> costs = costsOf(wantedOffer);
            if (canAfford(mod, costs)) {
                return new TradePlan(wantedIndex, true, costs,
                        "Buying " + displayName(wantedItem) + " (villager level " + level + ")");
            }
            // Short of emeralds/compass: prefer selling to the villager over a mining expedition.
            TradePlan sell = bestSellPlan(mod, offers);
            if (sell != null) {
                return sell;
            }
            pendingCosts = costs;
            setDebugState("Gathering payment for " + displayName(wantedItem));
            closeTradeScreen(mod);
            return null;
        }
        if (level >= requiredLevel) {
            // The wanted trade belongs to a level that has already been rolled and is not in the
            // list. No amount of further trading adds it: this villager cannot sell the item.
            Debug.logMessage("Trade: the level-" + level + " " + professionName() + " does not offer "
                    + displayName(wantedItem) + " (26.3 rolls 2 of N offers per level); moving on");
            markCurrentVillagerHopeless();
            return null;
        }
        TradePlan sell = bestSellPlan(mod, offers);
        if (sell != null) {
            return sell;
        }
        // No emerald-producing offer: buy an xp-granting item instead (expensive; capped by MAX_TRADES).
        TradePlan xp = bestXpPlan(mod, offers);
        if (xp != null) {
            return xp;
        }
        Debug.logWarning("Trade: no offer can be used to level this " + professionName() + " up");
        markCurrentVillagerHopeless();
        return null;
    }

    private Task tickMenuAction(AltoClef mod, MerchantMenu menu) {
        switch (menuAction) {
            case SELECT -> {
                if (!stepTimer.elapsed() || !mod.getSlotHandler().canDoSlotAction()) return null;
                setDebugState(pendingLabel);
                try {
                    sendSelectTrade(mod, pendingOfferIndex);
                } catch (Exception e) {
                    abortMenuCycle(mod, "select-trade packet failed: " + e.getMessage());
                    return null;
                }
                selectRetries++;
                menuAction = MenuAction.WAIT_RESULT;
                stepTimer.reset();
                return null;
            }
            case WAIT_RESULT -> {
                if (!stepTimer.elapsed()) return null;
                ItemStack result = menu.getSlot(tradeScreenResultSlot).getItem();
                if (result.isEmpty()) {
                    if (selectRetries < 3) {
                        menuAction = MenuAction.SELECT;
                        stepTimer.reset();
                        setDebugState("Waiting for the server to accept the offer");
                        return null;
                    }
                    abortMenuCycle(mod, "the trade never produced a result (payment or stock problem)");
                    return null;
                }
                if (!mod.getSlotHandler().canDoSlotAction()) return null;
                clickSlot(mod, tradeScreenResultSlot);
                pickupRetries++;
                menuAction = MenuAction.WAIT_PICKUP;
                stepTimer.reset();
                return null;
            }
            case WAIT_PICKUP -> {
                if (!stepTimer.elapsed()) return null;
                if (!StorageHelper.getItemStackInCursorSlot().isEmpty()) {
                    int storeSlot = findStoreWindowSlot(menu, StorageHelper.getItemStackInCursorSlot());
                    if (storeSlot >= 0 && mod.getSlotHandler().canDoSlotAction()) {
                        clickSlot(mod, storeSlot);
                        storeRetries++;
                        menuAction = MenuAction.WAIT_STORE;
                        stepTimer.reset();
                        setDebugState("Storing the traded item");
                        return null;
                    }
                    abortMenuCycle(mod, "no inventory slot accepted the traded item");
                    return null;
                }
                if (pickupRetries < 2) {
                    menuAction = MenuAction.WAIT_RESULT;
                    stepTimer.reset();
                    return null;
                }
                abortMenuCycle(mod, "clicking the result slot did nothing");
                return null;
            }
            case WAIT_STORE -> {
                if (!stepTimer.elapsed()) return null;
                if (StorageHelper.getItemStackInCursorSlot().isEmpty()) {
                    onTradeCompleted(mod);
                    return null;
                }
                if (storeRetries < 3) {
                    int storeSlot = findStoreWindowSlot(menu, StorageHelper.getItemStackInCursorSlot());
                    if (storeSlot >= 0 && mod.getSlotHandler().canDoSlotAction()) {
                        clickSlot(mod, storeSlot);
                        storeRetries++;
                        stepTimer.reset();
                        return null;
                    }
                }
                abortMenuCycle(mod, "the traded item is stuck in the cursor");
                return null;
            }
        }
        return null;
    }

    private void onTradeCompleted(AltoClef mod) {
        // "The cursor emptied" is not "the trade happened": verify the result actually arrived.
        boolean verified = true;
        if (expectedTradeItem != null) {
            int now = mod.getItemStorage().getItemCountInventoryOnly(expectedTradeItem);
            if (now < expectedTradeCountBefore + expectedTradeDelta) {
                verified = false;
                Debug.logWarning("Trade: the click sequence completed but no "
                        + displayName(expectedTradeItem) + " arrived (" + expectedTradeCountBefore
                        + " -> " + now + ")");
            }
        }
        expectedTradeItem = null;
        if (!verified) {
            failedMenuCycles++;
            resetMenuState();
            closeTradeScreen(mod);
            postTradeTimer.reset();
            if (failedMenuCycles > 3) {
                markCurrentVillagerHopeless();
            }
            return;
        }
        tradesDone++;
        Debug.logMessage("Trade: trade #" + tradesDone + " succeeded (" + pendingLabel + ")");
        setDebugState("Traded " + tradesDone + " times");
        boolean buyWanted = pendingBuyWanted;
        resetMenuState();
        closeTradeScreen(mod);
        postTradeTimer.reset();
        if (buyWanted && super.isFinished()) {
            finishSuccessfully(mod);
        }
        // Otherwise the next tick re-opens the screen and re-decides from fresh offers.
    }

    /** Close the screen and start over on the next tick, after a short delay. */
    private void abortMenuCycle(AltoClef mod, String reason) {
        Debug.logWarning("Trade: " + reason);
        failedMenuCycles++;
        resetMenuState();
        closeTradeScreen(mod);
        postTradeTimer.reset();
        if (failedMenuCycles > 3) {
            markCurrentVillagerHopeless();
        }
    }

    private void resetMenuState() {
        menuAction = MenuAction.NONE;
        pendingOfferIndex = -1;
        pendingBuyWanted = false;
        pendingLabel = "";
        selectRetries = 0;
        pickupRetries = 0;
        storeRetries = 0;
        tradeScreenResultSlot = -1;
        expectedTradeItem = null;
    }

    private void finishSuccessfully(AltoClef mod) {
        if (phase == Phase.DONE) return;
        phase = Phase.DONE;
        Debug.logMessage("Trade: obtained " + wantedCount + "x " + displayName(wantedItem)
                + " after " + tradesDone + " trade(s)");
        setDebugState("Traded " + tradesDone + " times; got " + displayName(wantedItem));
        closeTradeScreen(mod);
    }

    private void markCurrentVillagerHopeless() {
        UUID id = targetVillagerId;
        if (id == null && targetVillager != null) id = targetVillager.getUUID();
        if (id != null) hopelessVillagers.add(id);
        targetVillager = null;
        targetVillagerId = null;
        targetHomePos = null;
        pendingCosts = List.of();
        resetMenuState();
        closeTradeScreen(AltoClef.getInstance());
        phase = Phase.NEXT;
    }

    private void giveUp(AltoClef mod, String reason) {
        if (gaveUp) return;
        gaveUp = true;
        giveUpReason = reason;
        phase = Phase.DONE;
        closeTradeScreen(mod);
        setDebugState("Gave up: " + reason);
        Debug.logMessage("Trade: giving up on " + displayName(wantedItem) + " — " + reason);
        Debug.logWarning("Trade: gave up on " + displayName(wantedItem) + " — " + reason);
    }

    // ─────────────────────────────────────── villager search ───────────────────────────────────

    private void adoptVillager(Villager villager) {
        targetVillager = villager;
        targetVillagerId = villager.getUUID();
        targetHomePos = villager.blockPosition();
        lostVillagerWaitTicks = 0;
        openAttempts = 0;
        openAttemptTimer.reset();
        failedMenuCycles = 0;
        employmentAttempted = false;
    }

    private Villager findRememberedVillager(AltoClef mod) {
        if (targetVillagerId == null) return null;
        for (Villager villager : trackedVillagers(mod)) {
            if (villager.isAlive() && targetVillagerId.equals(villager.getUUID())) {
                return villager;
            }
        }
        return null;
    }

    /** Nearest alive, adult, untried villager of the wanted profession and an allowed type. */
    private Villager findEligibleVillager(AltoClef mod) {
        Villager best = null;
        double bestDist = Double.POSITIVE_INFINITY;
        for (Villager villager : trackedVillagers(mod)) {
            if (!villager.isAlive() || villager.isBaby()) continue;
            if (hopelessVillagers.contains(villager.getUUID())) continue;
            if (!isWantedProfession(villager)) continue;
            if (!isAllowedType(villager)) continue;
            double dist = villager.distanceToSqr(mod.getPlayer());
            if (dist < bestDist) {
                bestDist = dist;
                best = villager;
            }
        }
        return best;
    }

    /** Nearest alive, adult villager with no profession and no failed employment attempt. */
    private Villager findJoblessVillager(AltoClef mod) {
        Villager best = null;
        double bestDist = Double.POSITIVE_INFINITY;
        for (Villager villager : trackedVillagers(mod)) {
            if (!villager.isAlive() || villager.isBaby()) continue;
            if (hopelessVillagers.contains(villager.getUUID())) continue;
            if (employmentTried.contains(villager.getUUID())) continue;
            VillagerData data = villager.getVillagerData();
            if (data == null || !data.profession().is(VillagerProfession.NONE)) continue;
            double dist = villager.distanceToSqr(mod.getPlayer());
            if (dist < bestDist) {
                bestDist = dist;
                best = villager;
            }
        }
        return best;
    }

    private List<Villager> trackedVillagers(AltoClef mod) {
        List<Villager> tracked = mod.getEntityTracker().getTrackedEntities(Villager.class);
        if (tracked == null) return List.of();
        // The tracker hands out its live list; copy it before sorting under concurrent changes.
        List<Villager> copy = new ArrayList<>(tracked);
        copy.sort(Comparator.comparingDouble(v -> v.distanceToSqr(mod.getPlayer())));
        return copy;
    }

    private boolean isWantedProfession(Villager villager) {
        VillagerData data = villager.getVillagerData();
        return data != null && data.profession().is(profession);
    }

    private boolean isAllowedType(Villager villager) {
        if (allowedVillagerTypes.isEmpty()) return true;
        VillagerData data = villager.getVillagerData();
        if (data == null) return false;
        for (ResourceKey<VillagerType> type : allowedVillagerTypes) {
            if (data.type().is(type)) return true;
        }
        return false;
    }

    private int villagerLevel(Villager villager) {
        if (villager == null) return 0;
        VillagerData data = villager.getVillagerData();
        return data == null ? 0 : data.level();
    }

    private int currentVillagerLevel(MerchantMenu menu) {
        return Math.max(menu.getTraderLevel(), villagerLevel(targetVillager));
    }

    // ───────────────────────────────────────── offer helpers ───────────────────────────────────

    private static int findOfferFor(MerchantOffers offers, Item resultItem) {
        int fallback = -1;
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            if (offer == null || offer.getResult().getItem() != resultItem) continue;
            if (!offer.isOutOfStock()) return i;
            if (fallback < 0) fallback = i;
        }
        return fallback;
    }

    /**
     * The emerald-producing offer that levels the villager up fastest; the one we can already pay for
     * wins over a higher-xp one we would have to go and gather for.
     */
    private TradePlan bestSellPlan(AltoClef mod, MerchantOffers offers) {
        TradePlan bestAffordable = null;
        int bestAffordableXp = -1;
        TradePlan bestAny = null;
        int bestAnyXp = -1;
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            if (offer == null || offer.isOutOfStock()) continue;
            if (offer.getResult().getItem() != Items.EMERALD) continue;
            int xp = offer.getXp();
            if (xp <= 0) continue;
            List<Cost> costs = costsOf(offer);
            String label = "Selling " + describeCosts(costs) + " for emeralds (lvl up, +" + xp + " xp)";
            TradePlan plan = new TradePlan(i, false, costs, label);
            if (canAfford(mod, costs) && xp > bestAffordableXp) {
                bestAffordableXp = xp;
                bestAffordable = plan;
            }
            if (xp > bestAnyXp) {
                bestAnyXp = xp;
                bestAny = plan;
            }
        }
        return bestAffordable != null ? bestAffordable : bestAny;
    }

    /** An affordable offer that grants xp, for when the villager has no emerald-producing offer. */
    private TradePlan bestXpPlan(AltoClef mod, MerchantOffers offers) {
        TradePlan best = null;
        int bestXp = -1;
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            if (offer == null || offer.isOutOfStock() || offer.getXp() <= 0) continue;
            List<Cost> costs = costsOf(offer);
            if (!canAfford(mod, costs)) continue;
            if (offer.getXp() > bestXp) {
                bestXp = offer.getXp();
                best = new TradePlan(i, false, costs,
                        "Trading " + describeCosts(costs) + " for xp (+" + offer.getXp() + ")");
            }
        }
        return best;
    }

    private static List<Cost> costsOf(MerchantOffer offer) {
        List<Cost> costs = new ArrayList<>(2);
        addCost(costs, offer.getItemCostA(), offer.getCostA());
        Optional<ItemCost> costB = offer.getItemCostB();
        costB.ifPresent(itemCost -> addCost(costs, itemCost, offer.getCostB()));
        return costs;
    }

    private static void addCost(List<Cost> costs, ItemCost itemCost, ItemStack effectivePrice) {
        if (itemCost == null) return;
        Item item = itemCost.item().value();
        // tryMoveItems moves the BASE count into the payment slots while the offer accepts the
        // modified price, so demand the larger of the two: never open a trade we cannot complete.
        int count = Math.max(itemCost.count(), effectivePrice.isEmpty() ? 0 : effectivePrice.getCount());
        if (count <= 0) return;
        costs.add(new Cost(item, count));
    }

    private static boolean canAfford(AltoClef mod, List<Cost> costs) {
        for (Cost cost : costs) {
            if (mod.getItemStorage().getItemCountInventoryOnly(cost.item()) < cost.count()) {
                return false;
            }
        }
        return true;
    }

    private static List<Cost> missingCosts(AltoClef mod, List<Cost> costs) {
        List<Cost> missing = new ArrayList<>();
        for (Cost cost : costs) {
            int have = mod.getItemStorage().getItemCountInventoryOnly(cost.item());
            if (have < cost.count()) {
                missing.add(new Cost(cost.item(), cost.count() - have));
            }
        }
        return missing;
    }

    // ───────────────────────────────────── menu / slot plumbing ────────────────────────────────

    private static MerchantMenu getOpenMerchantMenu(AltoClef mod) {
        LocalPlayer player = mod.getPlayer();
        if (player == null) return null;
        if (player.containerMenu instanceof MerchantMenu menu) {
            return menu;
        }
        return null;
    }

    /** The result slot is the {@link MerchantResultSlot} in the menu (vanilla puts it at index 2). */
    private static int resultSlotIndex(MerchantMenu menu) {
        for (int i = 0; i < menu.slots.size(); i++) {
            if (menu.slots.get(i) instanceof MerchantResultSlot) {
                return i;
            }
        }
        Debug.logWarning("Trade: no MerchantResultSlot found in the trade menu; assuming slot 2");
        return 2;
    }

    private static void sendSelectTrade(AltoClef mod, int offerIndex) {
        LocalPlayer player = mod.getPlayer();
        if (player == null) return;
        player.connection.send(new ServerboundSelectTradePacket(offerIndex));
    }

    /** Click a window slot of the currently open menu by raw window index. */
    private static void clickSlot(AltoClef mod, int windowSlot) {
        mod.getSlotHandler().clickSlot(new PlayerSlot(windowSlot), 0, ContainerInput.PICKUP);
    }

    /**
     * A window slot of the open trade menu that can take the stack: an empty player-inventory slot
     * first, then a player-inventory slot holding the same item with room. -1 when none exists.
     */
    private static int findStoreWindowSlot(MerchantMenu menu, ItemStack stack) {
        int stackable = -1;
        for (int i = 0; i < menu.slots.size(); i++) {
            net.minecraft.world.inventory.Slot slot = menu.slots.get(i);
            if (!(slot.container instanceof Inventory)) continue;
            ItemStack inSlot = slot.getItem();
            if (inSlot.isEmpty()) {
                return i;
            }
            if (stackable < 0
                    && ItemStack.isSameItemSameComponents(inSlot, stack)
                    && inSlot.getCount() + stack.getCount() <= inSlot.getMaxStackSize()) {
                stackable = i;
            }
        }
        return stackable;
    }

    private void closeTradeScreen(AltoClef mod) {
        LocalPlayer player = mod.getPlayer();
        if (player == null) return;
        try {
            if (player.containerMenu instanceof MerchantMenu) {
                player.closeContainer();
            }
        } catch (Exception e) {
            Debug.logWarning("Trade: closing the trade screen failed: " + e.getMessage());
        }
    }

    private boolean hasJobSiteNear(AltoClef mod, Villager villager) {
        if (mod.getBlockScanner() == null || jobSiteBlock == null) return false;
        for (BlockPos pos : mod.getBlockScanner().getKnownLocations(jobSiteBlock)) {
            if (pos.closerToCenterThan(villager.position(), 8)) {
                return true;
            }
        }
        return false;
    }

    // ─────────────────────────────────────────── helpers ──────────────────────────────────────

    private String professionName() {
        Identifier id = profession.identifier();
        return id == null ? "villager" : id.getPath();
    }

    private static String describeCosts(List<Cost> costs) {
        if (costs.isEmpty()) return "items";
        StringBuilder result = new StringBuilder();
        for (Cost cost : costs) {
            if (result.length() > 0) result.append(" + ");
            result.append(cost.count()).append(' ').append(displayName(cost.item()));
        }
        return result.toString();
    }

    private static String displayName(Item item) {
        try {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null) return id.getPath();
        } catch (Exception ignored) {
            // fall through to the description id
        }
        try {
            return item.getDescriptionId();
        } catch (Exception e) {
            return String.valueOf(item);
        }
    }

    private static double horizontalDistance(AltoClef mod, BlockPos pos) {
        BlockPos player = mod.getPlayer().blockPosition();
        double dx = player.getX() - pos.getX();
        double dz = player.getZ() - pos.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}

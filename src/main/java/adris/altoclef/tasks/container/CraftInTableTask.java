package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.recipemanager.WrappedRecipeEntry;
import adris.altoclef.tasks.CraftGenericManuallyTask;
import adris.altoclef.tasks.CraftGenericWithRecipeBooksTask;
import adris.altoclef.tasks.CraftInInventoryTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.movement.TimeoutWanderTask;
import adris.altoclef.tasks.resources.CollectRecipeCataloguedResourcesTask;
import adris.altoclef.tasks.slot.MoveInaccessibleItemToInventoryTask;
import adris.altoclef.tasks.slot.ReceiveCraftingOutputSlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.JankCraftingRecipeMapping;
import adris.altoclef.util.RecipeTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.CraftingTableSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.time.TimerGame;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Crafts an item in a crafting table, obtaining and placing the table down if none was found.
 */
public class CraftInTableTask extends ResourceTask {

    /**
     * Scanner lookups for a crafting table: asked / found, and the last distance. Read as tbl.
     *
     * <p>Declared here rather than on DoCraftInTableTask, which does the incrementing, because that
     * class is package-private and the py4j bridge lives outside the package. Same package, so the
     * writes still resolve.
     */
    public static volatile int tblAsked, tblFound, tblLastDist;

    private final RecipeTarget[] targets;

    private final DoCraftInTableTask craftTask;

    public CraftInTableTask(RecipeTarget[] targets) {
        super(extractItemTargets(targets));
        this.targets = targets;
        craftTask = new DoCraftInTableTask(this.targets);
    }

    public CraftInTableTask(RecipeTarget target, boolean collect, boolean ignoreUncataloguedSlots) {
        super(new ItemTarget(target.getOutputItem(), target.getTargetCount()));
        targets = new RecipeTarget[]{target};
        craftTask = new DoCraftInTableTask(targets, collect, ignoreUncataloguedSlots);
    }

    public CraftInTableTask(RecipeTarget target) {
        this(target, true, true);
    }

    /**
     * Extracts item targets from recipe targets.
     *
     * @param recipeTargets The array of recipe targets.
     * @return The array of item targets.
     */
    private static ItemTarget[] extractItemTargets(RecipeTarget[] recipeTargets) {
        // Use Java streams to map each recipe target to a new item target
        return Arrays.stream(recipeTargets)
                .map(t -> new ItemTarget(t.getOutputItem(), t.getTargetCount()))
                .toArray(ItemTarget[]::new);
    }

    /**
     * Determines whether the player should avoid picking up items.
     *
     * @param mod The AltoClef mod instance.
     * @return true if the player should avoid picking up items, false otherwise.
     */
    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    /**
     * Called when the resource starts.
     *
     * @param mod The AltoClef mod instance.
     */
    @Override
    protected void onResourceStart(AltoClef mod) {

    }

    /**
     * This method is called on each tick of the resource manager.
     * It returns the task that should be executed on each tick.
     *
     * @param mod The instance of the AltoClef mod.
     * @return The task to be executed on each tick.
     */
    @Override
    protected Task onResourceTick(AltoClef mod) {
        return craftTask;
    }

    /**
     * Override method called when the resource stops.
     *
     * @param mod           The AltoClef mod.
     * @param interruptTask The interrupt task.
     */
    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        // Get the item stack in the cursor slot.
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();

        // If the cursor stack is not empty, handle it.
        if (!cursorStack.isEmpty()) {
            // Find a slot in the player inventory that can fit the cursor stack.
            Optional<Slot> moveToSlot = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);

            // If a slot is found, pick up the item from the cursor slot and move it to the found slot.
            moveToSlot.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, ContainerInput.PICKUP));

            // If the item can be thrown away, pick up the item from the cursor slot and throw it away.
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, ContainerInput.PICKUP);
            }

            // Find the garbage slot and move the item from the cursor slot to the garbage slot.
            Optional<Slot> garbageSlot = StorageHelper.getGarbageSlot(mod);
            garbageSlot.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, ContainerInput.PICKUP));
        } else {
            // If the cursor stack is empty, close the screen.
            StorageHelper.closeScreen();
        }

        // Pick up an undefined slot.
        mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, ContainerInput.PICKUP);
    }

    /**
     * Checks if the given ResourceTask is equal to this CraftInTableTask.
     *
     * @param other The ResourceTask to compare with.
     * @return true if the ResourceTask is a CraftInTableTask and its craftTask is equal to this task's craftTask, false otherwise.
     */
    @Override
    protected boolean isEqualResource(ResourceTask other) {
        // Check if the other task is an instance of CraftInTableTask
        if (other instanceof CraftInTableTask task) {
            // Compare the craftTask of the two tasks
            return craftTask.isEqual(task.craftTask);
        }
        // The other task is not a CraftInTableTask, return false
        return false;
    }

    /**
     * ⛔ A CRAFT TASK CRAFTS; IT DOES NOT DIG ACROSS THE WORLD FOR A DROPPED COPY (G93, 2026-09-18).
     * ResourceTask's pickup-before-craft returns a PickupDroppedItemTask for ANY drop of the target
     * while getPickupRange() allows it, and the default is getResourcePickupRange() == -1, i.e.
     * UNLIMITED -- range < 0 in ResourceTask.onTick is always true. The third 60-minute run spent
     * its first seven minutes chasing a wooden_pickaxe DROP a previous life left ten blocks
     * underground ("Getting to drop item.minecraft.wooden_pickaxe at 1236,56,-1406 (dig/build
     * allowed)"), digging down for it, while spruce to craft one stood across the lake. Its sibling
     * CraftInInventoryTask already returns 0 here (craft, do not scavenge); CraftInTableTask simply
     * never got the same override. Matching it: a table craft does not chase a dropped copy -- it
     * has, or gathers, the materials and makes one. (A drop still gets picked up by the ordinary
     * PickupDroppedItemTask paths when THAT is the goal; this only stops a CRAFT from detouring.)
     */
    @Override
    protected double getPickupRange(AltoClef mod) {
        return 0;
    }

    /**
     * Returns the debug string name of the craft task.
     * If the craft task is not null, it calls the toDebugString() method of the craft task and returns the result.
     * Otherwise, it returns null.
     *
     * @return the debug string name of the craft task, or null if the craft task is null.
     */
    @Override
    protected String toDebugStringName() {
        return (craftTask != null) ? craftTask.toDebugString() : null;
    }

    /**
     * Returns a copy of the recipe targets.
     *
     * @return The recipe targets.
     */
    public RecipeTarget[] getRecipeTargets() {
        return Arrays.copyOf(targets, targets.length);
    }
}


class DoCraftInTableTask extends DoStuffInContainerTask {

    // How long the table may go without a grid/output change before the stuck detector fires.
    // Depositing a large batch is many right-clicks and can legitimately take a while (reported by
    // the operator: "give it longer in the crafting window, they could be depositing a large
    // quantity"). 30 s of NO progress, plus the move delay, is still short enough to catch a real
    // stall but long enough not to interrupt a slow fill.
    private final float CRAFT_RESET_TIMER_BONUS_SECONDS = 30;

    private final RecipeTarget[] _targets;

    private final boolean _collect;

    private final CollectRecipeCataloguedResourcesTask _collectTask;
    private final TimerGame _craftResetTimer = new TimerGame(CRAFT_RESET_TIMER_BONUS_SECONDS);

    // ⛔ THE RECIPE-BOOK CLICK CAN BE SILENTLY REFUSED. `handlePlaceRecipe` sends a packet and
    // returns; if the server rejects it (recipe not unlocked, menu mismatch, a 26.2/26.3 difference)
    // nothing happens and this task used to sit at an open table doing nothing. Watch the grid: if
    // the signature has not changed shortly after the click, stop using the book for this craft and
    // let CraftGenericManuallyTask place the items by hand.
    private static final long RECIPE_BOOK_FALLBACK_MS = 1500L;
    private long _recipeBookSentAt = 0L;
    private long _recipeBookSentSig = 0L;
    private boolean _recipeBookDisabled = false;
    private int _craftCount;
    /** Last observed craft-progress signature; see {@link #craftProgressSignature}. */
    private long _lastCraftProgress = Long.MIN_VALUE;

    public DoCraftInTableTask(RecipeTarget[] targets, boolean collect, boolean ignoreUncataloguedSlots) {
        super(Blocks.CRAFTING_TABLE, new ItemTarget("crafting_table"));
        _collectTask = new CollectRecipeCataloguedResourcesTask(false, targets);
        _targets = targets;
        _collect = collect;
    }

    public DoCraftInTableTask(RecipeTarget[] targets) {
        this(targets, true, false);
    }

    /**
     * Override method called when the mod starts.
     * Refactored to handle item management and screen closing.
     * Resets the collect task.
     */
    @Override
    protected void onStart() {
        super.onStart();
        AltoClef mod = AltoClef.getInstance();
        // Save the current behaviour and craft count
        mod.getBehaviour().push();
        _craftCount = 0;

        // Check if there is an item in the cursor slot
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();

        if (!cursorStack.isEmpty()) {
            // Move the item to a slot in the player's inventory that can fit it
            Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            moveTo.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, ContainerInput.PICKUP));

            // Check if the item can be thrown away
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                // Throw away the item
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, ContainerInput.PICKUP);
            }

            // Move the item to the garbage slot
            StorageHelper.getGarbageSlot(mod).ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, ContainerInput.PICKUP));

            // Clear the cursor slot
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, ContainerInput.PICKUP);
        } else {
            // Close the screen if there is no item in the cursor slot
            StorageHelper.closeScreen();
        }

        // Reset the collect task
        _collectTask.reset();

        // Add protected items to the behaviour
        mod.getBehaviour().addProtectedItems(getMaterialsArray());
    }

    /**
     * This method is called when the task is interrupted or stopped.
     * It performs the necessary actions to handle the interruption or stopping of the task.
     *
     * @param interruptTask The task that caused the interruption, or null if the task was stopped manually.
     */
    @Override
    protected void onStop(Task interruptTask) {
        // Get the item stack in the cursor slot
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        AltoClef mod = AltoClef.getInstance();

        // If the cursor stack is empty, close the screen
        if (cursorStack.isEmpty()) {
            StorageHelper.closeScreen();
        } else {
            // Get a slot that can fit the cursor stack
            Optional<Slot> moveToSlot = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);

            // If a slot is found, move the cursor stack to that slot
            moveToSlot.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, ContainerInput.PICKUP));

            // If the cursor stack can be thrown away, throw it away
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, ContainerInput.PICKUP);
            }

            // Get the garbage slot
            Optional<Slot> garbageSlot = StorageHelper.getGarbageSlot(mod);

            // If a garbage slot is found, move the cursor stack to that slot
            garbageSlot.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, ContainerInput.PICKUP));

            // Move the cursor stack to an undefined slot
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, ContainerInput.PICKUP);
        }

        // Call the onStop method of the super class
        super.onStop(interruptTask);

        // Pop the behaviour from the stack
        mod.getBehaviour().pop();
    }

    /**
     * This method is called periodically to perform crafting-related tasks.
     *
     * @return The next task to execute.
     */
    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();

        // Avoid breaking crafting tables
        List<BlockPos> craftingTablePositions = mod.getBlockScanner().getKnownLocations(Blocks.CRAFTING_TABLE);
        for (BlockPos craftingTablePos : craftingTablePositions) {
            mod.getBehaviour().avoidBlockBreaking(craftingTablePos);
        }

        // Check if the player inventory is open and the cursor slot is empty
        if (StorageHelper.isPlayerInventoryOpen() && StorageHelper.getItemStackInCursorSlot().isEmpty()) {
            // Get the item in the craft output slot
            Item outputItem = StorageHelper.getItemStackInSlot(PlayerSlot.CRAFT_OUTPUT_SLOT).getItem();
            // Check if the output item matches any of the targets and the target count is not reached
            for (RecipeTarget target : _targets) {
                if (target.getOutputItem() == outputItem && mod.getItemStorage().getItemCount(target.getOutputItem()) < target.getTargetCount()) {
                    return new ReceiveCraftingOutputSlotTask(PlayerSlot.CRAFT_OUTPUT_SLOT, target.getTargetCount());
                }
            }
        }

        // Check if we need to collect items and the collect task is not finished
        if (_collect && !_collectTask.isFinished() && !StorageHelper.hasRecipeMaterialsOrTarget(mod, _targets)) {
            // ⛔ CLOSE THE TABLE BEFORE GOING SHOPPING (2026-10-23). This returned the collect task
            // while the crafting screen was still open. Altoclef's movement does not go through the
            // GUI, so the bot walked away with the crafting table screen open: the screen (and this
            // task's own "Using crafting_table..." state) stayed up while it wandered, which looks
            // exactly like "idling in the crafting table" when the real problem is that it ran out of
            // ingredients and never closed the table. Close first, gather, then the parent reopens.
            if (isContainerOpen(mod)) {
                StorageHelper.closeScreen();
                return null;
            }
            return _collectTask;
        }

        // Reset the craft reset timer if the container is not open
        if (!isContainerOpen(mod)) {
            _craftResetTimer.reset();
        }

        // Check if there is any inaccessible item in the recipes and move it to the inventory
        if (!thisOrChildSatisfies(task -> task instanceof CraftInInventoryTask)) {
            for (RecipeTarget target : _targets) {
                for (int slot = 0; slot < target.getRecipe().getSlotCount(); ++slot) {
                    ItemTarget toCheck = target.getRecipe().getSlot(slot);
                    if (StorageHelper.isItemInaccessibleToContainer(mod, toCheck)) {
                        return new MoveInaccessibleItemToInventoryTask(toCheck);
                    }
                }
            }
        }

        // Call the parent method
        return super.onTick();
    }

    /**
     * Checks if the given DoStuffInContainerTask is equal to this task.
     *
     * @param other The other DoStuffInContainerTask to compare.
     * @return True if the tasks are equal, False otherwise.
     */
    @Override
    protected boolean isSubTaskEqual(DoStuffInContainerTask other) {
        // Check if the other task is an instance of DoCraftInTableTask
        if (other instanceof DoCraftInTableTask task) {
            // Compare the targets arrays of the two tasks
            return Arrays.equals(task._targets, _targets);
        }
        // The other task is not an instance of DoCraftInTableTask, so they are not equal
        return false;
    }

    /**
     * Checks if the container is open.
     *
     * @param mod The AltoClef mod instance.
     * @return True if the container is open, false otherwise.
     */
    @Override
    protected boolean isContainerOpen(AltoClef mod) {
        return mod.getPlayer().containerMenu instanceof CraftingMenu;
    }

    /**
     * Executes the container subtask.
     *
     * @param mod The AltoClef mod instance.
     * @return The subtask to be executed.
     */
    @Override
    protected Task containerSubTask(AltoClef mod) {
        // Calculate the interval based on the container item move delay and a bonus duration
        float interval = mod.getModSettings().getContainerItemMoveDelay() * 20 + CRAFT_RESET_TIMER_BONUS_SECONDS;
        _craftResetTimer.setInterval(interval);

        // ⛔ THE STUCK-DETECTOR MUST NOT FIRE WHILE THE CRAFT IS MAKING PROGRESS. It used to wander
        // off 12 s after the table opened, no matter what the craft was doing -- so a slow-but-fine
        // fill (putting a stack into three slots is one right-click per item when the stack is
        // larger than the batch, which easily passes 12 s) got interrupted, the screen closed, the
        // grid was put back, and it started over: "it almost crafts it, then wanders off and keeps
        // cycling that". Reset the timer whenever the grid or the output actually changes, so it
        // only elapses when the craft is genuinely standing still.
        long progress = craftProgressSignature(mod);
        if (progress != _lastCraftProgress) {
            _lastCraftProgress = progress;
            _craftResetTimer.reset();
        }

        // A recipe-book click that changed nothing is a failed click, not a slow craft.
        if (_recipeBookSentAt != 0L) {
            long sigNow = craftProgressSignature(mod);
            if (sigNow != _recipeBookSentSig) {
                _recipeBookSentAt = 0L;
            } else if (System.currentTimeMillis() - _recipeBookSentAt > RECIPE_BOOK_FALLBACK_MS) {
                _recipeBookDisabled = true;
                _recipeBookSentAt = 0L;
                adris.altoclef.Debug.logMessage("CraftInTable: recipe book click changed nothing; placing by hand");
                setDebugState("Recipe book did nothing — placing items by hand");
            } else {
                // ⛔ WAIT FOR THE CLICK ALREADY IN FLIGHT (2026-10-22). This used to fall through to
                // the send below, which re-armed _recipeBookSentAt to "now" EVERY TICK — so the
                // 1.5 s fallback above could never elapse and the task idled at an open crafting
                // table for ever (operator: "@get bread 20 ... it's idling in the crafting table").
                // A refused recipe-book click must run its timeout and then hand over to the manual
                // placer, instead of being re-sent on every tick.
                setDebugState("Waiting for the recipe book...");
                return null;
            }
        }

        // ⛔ DO NOT WALK AWAY WHEN THE TIMER ELAPSES. It used to return a TimeoutWanderTask(5),
        // which walked off, came back, reopened the table and idled -- the exact "it loops walking
        // away and opening it" report. Idling at the table for another interval is strictly better:
        // the parent ResourceTask's progress checker is what aborts a genuinely stuck craft.
        if (_craftResetTimer.elapsed()) {
            _craftResetTimer.reset();
            setDebugState("Craft idle — waiting for items");
            return null;
        }

        // Iterate through each target recipe
        for (RecipeTarget target : _targets) {
            // Check if the output item count meets the target count
            if (mod.getItemStorage().getItemCount(target.getOutputItem()) >= target.getTargetCount()) {
                continue;
            }

            // Get the recipe to send based on the target recipe and output item
            Optional<WrappedRecipeEntry> recipeToSend = JankCraftingRecipeMapping.getMinecraftMappedRecipe(target.getRecipe(), target.getOutputItem());

            // Get the client player entity
            LocalPlayer player = Minecraft.getInstance().player;

            // If crafting book is enabled, the recipe to send exists, and the player has the recipe in their recipe book, return a CraftGenericWithRecipeBooksTask
            // Same algorithm CraftGenericWithRecipeBooksTask already proved correct for the
            // 2x2 inventory-grid case, adapted for the fact that -- unlike that task, called
            // standalone with no screen open -- this method only ever runs once
            // DoStuffInContainerTask has already opened the crafting TABLE
            // (isContainerOpen() gates every call here). Opening InventoryScreen the way that
            // task does would replace the already-open CraftingScreenHandler with the small
            // 2x2 grid and desync isContainerOpen() on the very next tick -- so this sends
            // clickRecipe against the screen that is ALREADY open instead of opening one.
            if (!_recipeBookDisabled
                    && mod.getModSettings().shouldUseCraftingBookToCraft() && player != null
                    && mod.getWorld() != null && mod.getSlotHandler().canDoSlotAction()) {
                net.minecraft.util.context.ContextMap ctx =
                        net.minecraft.world.item.crafting.display.SlotDisplayContext.fromLevel(mod.getWorld());
                net.minecraft.world.entity.player.StackedItemContents finder = new net.minecraft.world.entity.player.StackedItemContents();
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    finder.accountSimpleStack(player.getInventory().getItem(i));
                }
                for (net.minecraft.client.gui.screens.recipebook.RecipeCollection col
                        : player.getRecipeBook().getCollections()) {
                    col.selectRecipes(finder, e -> true);
                    for (net.minecraft.world.item.crafting.display.RecipeDisplayEntry entry : col.getRecipes()) {
                        for (ItemStack shown : entry.resultItems(ctx)) {
                            if (shown.getItem() == target.getOutputItem() && col.isCraftable(entry.id())) {
                                mod.getController().handlePlaceRecipe(
                                        player.containerMenu.containerId, entry.id(), true);
                                mod.getSlotHandler().registerSlotAction();
                                _recipeBookSentAt = System.currentTimeMillis();
                                _recipeBookSentSig = craftProgressSignature(mod);
                                return null;
                            }
                        }
                    }
                }
            }

            // Return a CraftGenericManuallyTask by default
            return new CraftGenericManuallyTask(target);
        }

        return null;
    }

    /**
     * A value that changes whenever the craft does something: the counts in this recipe's grid input
     * slots, plus the inventory count of the output. Equal from one tick to the next means the craft
     * is standing still; different means it is working and must not be timed out.
     */
    private long craftProgressSignature(AltoClef mod) {
        long sig = 17L;
        for (RecipeTarget target : _targets) {
            for (int i = 0; i < target.getRecipe().getSlotCount(); ++i) {
                Slot slot = CraftingTableSlot.getInputSlot(i, target.getRecipe().isBig());
                sig = sig * 31L + StorageHelper.getItemStackInSlot(slot).getCount();
            }
            sig = sig * 31L + mod.getItemStorage().getItemCount(target.getOutputItem());
        }
        return sig;
    }

    /**
     * Checks if the specified mod is finished.
     *
     * @return True if the mod is finished, false otherwise.
     */
    @Override
    public boolean isFinished() {
        // ⛔ A CRAFT TASK IS DONE WHEN ITS OUTPUT IS IN THE INVENTORY (2026-10-23).
        // `_craftCount` is never incremented anywhere, so the old check was false for ever. When the
        // requested craft was already satisfied (the operator's 20 bread against a "craft 12 more"
        // target) containerSubTask skipped every target, returned null, and the task sat at the open
        // crafting table indefinitely — exactly the reported "idling in the crafting table".
        for (RecipeTarget target : _targets) {
            if (AltoClef.getInstance().getItemStorage().getItemCount(target.getOutputItem())
                    < target.getTargetCount()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the cost to make a new AltoClef mod.
     *
     * @param mod The AltoClef mod instance.
     * @return The cost to make a new AltoClef mod.
     */
    @Override
    protected double getCostToMakeNew(AltoClef mod) {
        // Get the nearest crafting table.
        Optional<BlockPos> closestCraftingTable = mod.getBlockScanner().getNearestBlock(Blocks.CRAFTING_TABLE);
        // THE DECISION THAT craft_at_distant_table TURNS ON, AND IT WAS NEVER MEASURED.
        // If the scanner does not KNOW about a table, this returns a finite cost and the bot sets
        // out to CRAFT one -- and that course hands it three planks and two sticks on purpose, one
        // plank short of a table, so it can never finish and ends up wandering. That matches the
        // failure exactly: the bot stopped 19.7 blocks from a table it had 300 seconds to reach.
        // Whether the scanner sees a table 28 blocks away is therefore the whole question, and it
        // has been an assumption until now.
        CraftInTableTask.tblAsked++;
        if (closestCraftingTable.isPresent()) {
            CraftInTableTask.tblFound++;
            CraftInTableTask.tblLastDist = (int) Math.round(Math.sqrt(
                    closestCraftingTable.get().distToCenterSqr(mod.getPlayer().position())));
        }

        // If a crafting table is within 40 blocks of the player, return positive infinity.
        if (closestCraftingTable.isPresent() && closestCraftingTable.get().closerToCenterThan(mod.getPlayer().position(), 40)) {
            return Double.POSITIVE_INFINITY;
        }

        // If the mod has logs or enough planks, return a cost of 10.
        if (mod.getItemStorage().hasItem(ItemHelper.LOG) || mod.getItemStorage().getItemCount(ItemHelper.PLANKS) >= 4) {
            return 10;
        }

        // Otherwise, return a cost of 100.
        return 100;
    }

    /**
     * Returns an array of materials.
     *
     * @return the array of materials
     */
    private Item[] getMaterialsArray() {
        List<Item> result = new ArrayList<>();

        // Iterate over each target
        for (RecipeTarget target : _targets) {
            // Iterate over each slot in the recipe
            for (int i = 0; i < target.getRecipe().getSlotCount(); ++i) {
                ItemTarget materialTarget = target.getRecipe().getSlot(i);
                // Check if the material target is not null and has matches
                if (materialTarget != null && materialTarget.getMatches() != null) {
                    // Add all the matches to the result list
                    Collections.addAll(result, materialTarget.getMatches());
                }
            }
        }

        // Convert the result list to an array and return it
        return result.toArray(new Item[0]);
    }

}

package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasks.movement.GetCloseToBlockTask;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.slots.Slot;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PlaceSignTask extends Task {

    private final BlockPos _target;
    private final String _message;
    private Direction _dir = Direction.UP;

    private boolean _finished;

    public PlaceSignTask(BlockPos pos, String message) {
        _target = pos;
        _message = message;
    }

    public PlaceSignTask(BlockPos pos, Direction dir, String message) {
        _target = pos;
        _dir = dir;
        _message = message;
    }

    public PlaceSignTask(String message) {
        this(null, message);
    }

    public static boolean isSign(Block block) {
        for (Block check : ItemHelper.WOOD_SIGNS_ALL) {
            if (check == block) return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        _finished = false;
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();

        if (editingSign()) {
            return editSign(mod);
        }

        // Make sure we have a sign to place
        if (!StorageHelper.hasCataloguedItem(mod, "sign")) {
            return TaskCatalogue.getItemTask("sign", 1);
        }

        // Place sign
        if (placeAnywhere()) {
            return new PlaceBlockNearbyTask(ItemHelper.WOOD_SIGNS_ALL);
        } else {

            assert Minecraft.getInstance().level != null;
            BlockState b = Minecraft.getInstance().level.getBlockState(_target);

            if (!isSign(b.getBlock()) && !b.isAir() && b.getBlock() != Blocks.WATER && b.getBlock() != Blocks.LAVA) {
                return new DestroyBlockTask(_target);
            }

            BlockPos baseBlockPos = _target.offset(_dir.getOpposite().getUnitVec3i());
            BlockPos playerPos = mod.getPlayer().blockPosition();

            if (!(playerPos.getX() == _target.getX() && playerPos.getZ() == _target.getZ()))
                return new GetToBlockTask(_target);
            if (LookHelper.cleanLineOfSight(WorldHelper.toVec3d(_target), 2))
                return new InteractWithBlockTask(new ItemTarget("sign", 1), _dir, baseBlockPos, false);
            else
                return new GetCloseToBlockTask(_target);
        }
    }

    private Task editSign(AltoClef mod) {
        SignEditScreen screen = (SignEditScreen) Minecraft.getInstance().gui.screen();
        assert screen != null;

        StringBuilder currentLine = new StringBuilder();

        int lines = 0;

        final int SIGN_TEXT_MAX_WIDTH = 90;

        for (char c : _message.toCharArray()) {
            currentLine.append(c);

            if (c == '\n' || Minecraft.getInstance().font.width(currentLine.toString()) > SIGN_TEXT_MAX_WIDTH) {
                currentLine.delete(0, currentLine.length());
                if (c != '\n') {
                    currentLine.append(c);
                }
                lines++;
                if (lines >= 4) {
                    Debug.logWarning("Too much text to fit on sign! Got Cut off.");
                    break;
                }

                // Add newline
                screen.keyPressed(new net.minecraft.client.input.KeyEvent(257, 36, 0));
            }
            screen.charTyped(new net.minecraft.client.input.CharacterEvent((int) c));
        }
        screen.onClose();
        _finished = true;

        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef mod = AltoClef.getInstance();
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        if (!cursorStack.isEmpty()) {
            Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            moveTo.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, ContainerInput.PICKUP));
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, ContainerInput.PICKUP);
            }
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
            garbage.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, ContainerInput.PICKUP));
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, ContainerInput.PICKUP);
        } else {
            StorageHelper.closeScreen();
        }
    }

    @Override
    public boolean isFinished() {
        return _finished;
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof PlaceSignTask task) {
            if (!task._message.equals(_message)) return false;
            if ((task._target == null) != (_target == null)) return false;

            if (_dir != null && task._dir != null) {
                if (!task._dir.equals(_dir))
                    return false;
            }
            if (task._target != null) {
                return task._target.equals(_target);
            }
            return true;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        if (placeAnywhere()) {
            return "Place Sign Anywhere";
        }
        return "Place Sign at " + _target.toShortString();
    }

    private boolean placeAnywhere() {
        return _target == null;
    }

    private boolean editingSign() {
        return Minecraft.getInstance().gui.screen() instanceof SignEditScreen;
    }
}

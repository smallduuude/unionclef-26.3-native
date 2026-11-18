package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.commandsystem.*;
import adris.altoclef.commandsystem.args.ItemTargetArg;
import adris.altoclef.commandsystem.args.ListArg;
import adris.altoclef.commandsystem.exception.CommandException;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import java.util.List;

public class GetCommand extends Command {

    public GetCommand() throws CommandException {
        super("get", "Get an item/resource",
                new ListArg<>(new ItemTargetArg("stack"), "items")
        );
    }


    private void getItems(AltoClef mod, List<ItemTarget> items) {
        Task targetTask;
        if (items == null || items.isEmpty()) {
            mod.log("You must specify at least one item!");
            finish();
            return;
        }
        // ⛔ "@get bed 1" MUST GO AND GET A BED.
        //
        // This command used to mean "make sure I hold at least 1" — so with a bed already in the
        // pack the task was satisfied on its FIRST tick and the bot did nothing at all. Reported by
        // the operator as the command being ignored ("needs to get a bed, not be happy with the one
        // it already has"). A typed command asks for WORK, so the target is what is held NOW plus
        // what was asked for: the bot gathers n more, and every nested task still reads an ordinary
        // "ensure you hold N" goal — which is what makes this safe. Only this command changes;
        // internal ItemTargets used by other tasks are untouched.
        List<ItemTarget> wanted = new java.util.ArrayList<>(items.size());
        for (ItemTarget t : items) {
            int have = mod.getItemStorage().getItemCountInventoryOnly(t.getMatches());
            int want = have + Math.max(1, t.getTargetCount());
            wanted.add(t.isCatalogueItem()
                    ? new ItemTarget(t.getCatalogueName(), want)
                    : new ItemTarget(t.getMatches(), want));
        }
        if (wanted.size() == 1) {
            targetTask = TaskCatalogue.getItemTask(wanted.getFirst());
        } else {
            targetTask = TaskCatalogue.getSquashedItemTask(wanted.toArray(new ItemTarget[0]));
        }
        if (targetTask != null) {
            mod.runUserTask(targetTask, this::finish);
        } else {
            finish();
        }
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        List<ItemTarget> items = parser.get(List.class);

        getItems(mod, items);
    }
}
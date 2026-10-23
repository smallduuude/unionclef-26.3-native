package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.args.StringArg;
import adris.altoclef.commandsystem.exception.CommandException;
import adris.altoclef.tasks.movement.GetToBlockTask;
import adris.altoclef.tasks.movement.GoToStrongholdPortalTask;
import adris.altoclef.tasks.movement.LocateDesertTempleTask;
import adris.altoclef.util.helpers.StructureLocator;
import java.util.List;
import net.minecraft.client.Minecraft;

/**
 * Locate a generated structure and print its coordinates.
 *
 * <p><b>Cheat-free:</b> in singleplayer the client owns the integrated server, so this asks the
 * world generator directly (the same query {@code /locate structure} uses) instead of running the
 * command. That needs no cheats and no op. On a multiplayer server there is no integrated server to
 * ask: the command says so rather than pretending.
 *
 * <pre>
 *   @locate_structure minecraft:desert_pyramid
 *   @locate_structure minecraft:buried_treasure go
 * </pre>
 *
 * <p>The historical forms {@code @locate_structure desert_temple} and
 * {@code @locate_structure stronghold} keep their old hand-written tasks.
 */
public class LocateStructureCommand extends Command {

    public LocateStructureCommand() {
        super("locate_structure", "Locate a world generated structure (singleplayer; cheat-free).",
                new StringArg("structure"),
                new StringArg("action", "")
        );
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String structure = parser.get(String.class);
        String action = parser.get(String.class);

        // Historical enum values keep their hand-tuned tasks.
        if ("desert_temple".equalsIgnoreCase(structure)) {
            mod.runUserTask(new LocateDesertTempleTask(), this::finish);
            return;
        }
        if ("stronghold".equalsIgnoreCase(structure)) {
            mod.runUserTask(new GoToStrongholdPortalTask(1), this::finish);
            return;
        }

        String id = structure.contains(":") ? structure : "minecraft:" + structure;
        if (!StructureLocator.isAvailable()) {
            logError("Structure locating needs a singleplayer world (the client must own the "
                    + "integrated server). On a server it would need op/cheats, which this mod does "
                    + "not use.");
            finish();
            return;
        }

        // USER-FACING channel. Neither Debug.logMessage nor mod.log reaches chat at the default
        // "Important only" verbosity (measured on the smoke runs: the locator worked but every answer
        // was invisible, so the command looked ignored). Debug.logUserMessage is the channel that is
        // shown under both "All" and "Important only" and only silenced by "Off".
        Debug.logUserMessage("Locating " + id + " ...");
        StructureLocator.locate(mod, List.of(id), 100, false).whenComplete((found, error) -> {
            // The future completes on the integrated server thread; talk to the client only there.
            Minecraft.getInstance().execute(() -> {
                if (error != null) {
                    Debug.logUserMessage("Locate failed: " + error);
                    finish();
                    return;
                }
                if (found == null) {
                    Debug.logUserMessage("No " + id + " found within 100 chunks of you.");
                    finish();
                    return;
                }
                Debug.logUserMessage("Found " + found.structureId() + " at " + found.pos().getX() + ", "
                        + found.pos().getY() + ", " + found.pos().getZ());
                if ("go".equalsIgnoreCase(action)) {
                    mod.runUserTask(new GetToBlockTask(found.pos()), this::finish);
                } else {
                    finish();
                }
            });
        });
    }
}

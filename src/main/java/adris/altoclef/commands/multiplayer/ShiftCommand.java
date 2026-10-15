package adris.altoclef.commands.multiplayer;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.args.PlayerArg;
import adris.altoclef.commandsystem.args.StringArg;
import adris.altoclef.commandsystem.exception.CommandException;
import adris.altoclef.commandsystem.exception.RuntimeCommandException;
import adris.altoclef.tasks.entity.ShiftEntityTask;
import java.util.Optional;
import net.minecraft.world.entity.player.Player;

public class ShiftCommand extends Command {
    public ShiftCommand() {
        super("shift", "Shifts near a player (back/forward/any)",
                new PlayerArg("username"),
                new StringArg("type", "back"));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        String username = parser.get(String.class);
        String type = parser.get(String.class);

        if (username == null) {
            finish();
            return;
        }

        Optional<Player> target = mod.getEntityTracker().getPlayerEntity(username);
        if (target.isEmpty()) {
            throw new RuntimeCommandException("Player " + username + " not found!");
        }

        ShiftEntityTask.ShiftType shiftType = switch (type.toLowerCase()) {
            case "forward" -> ShiftEntityTask.ShiftType.Forward;
            case "any" -> ShiftEntityTask.ShiftType.Any;
            default -> ShiftEntityTask.ShiftType.Back;
        };

        mod.runUserTask(new ShiftEntityTask(target.get(), shiftType), this::finish);
    }
}

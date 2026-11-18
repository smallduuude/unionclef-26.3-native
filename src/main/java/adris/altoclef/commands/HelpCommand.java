package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.ui.MessagePriority;

public class HelpCommand extends Command {

    public HelpCommand() {
        super("help", "Lists all commands");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) {
        mod.log("########## HELP: ##########", MessagePriority.OPTIONAL);
        int padSize = 10;
        for (Command c : AltoClef.getCommandExecutor().allCommands()) {
            // Debug hooks and gimmicks stay usable by name; they are just not advertised.
            if (c.isHidden()) continue;
            StringBuilder line = new StringBuilder();
            //line.append("");
            for (String name : c.getNames()) {
                line.append(name).append(": ");
                int toAdd = padSize - name.length();
                line.append(" ".repeat(Math.max(0, toAdd)));
                line.append(c.getDescription());
                mod.log(line.toString(), MessagePriority.OPTIONAL);
            }
        }
        mod.log("###########################", MessagePriority.OPTIONAL);
        finish();
    }
}
package kaptainwutax.tungsten.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import kaptainwutax.tungsten.TungstenMod;
import kaptainwutax.tungsten.commandsystem.Command;
import kaptainwutax.tungsten.commandsystem.CommandException;
import kaptainwutax.tungsten.task.PunkPlayerTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;

public class PunkPlayerCommand extends Command {

    public PunkPlayerCommand(TungstenMod mod) throws CommandException {
        super("punkPlayer", "Hunt and fight a player (A* approach + MPC combat)", mod);
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        SuggestionProvider<SharedSuggestionProvider> playerSuggestions = (ctx, sb) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) {
                String input = sb.getRemaining().toLowerCase();
                for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
                    //#if MC < 12111
                    //$$ String name = entry.getProfile().getName();
                    //#else
                    String name = entry.getProfile().name();
                    //#endif
                    if (name.toLowerCase().startsWith(input)) {
                        sb.suggest(name);
                    }
                }
            }
            return sb.buildFuture();
        };

        // ;punkPlayer <name>
        builder.then(argument("name", StringArgumentType.word())
                .suggests(playerSuggestions)
                .executes(context -> {
                    String name = StringArgumentType.getString(context, "name");
                    PunkPlayerTask.start(name);
                    return SINGLE_SUCCESS;
                }));
    }
}

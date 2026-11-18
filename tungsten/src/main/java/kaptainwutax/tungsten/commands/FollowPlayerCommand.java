package kaptainwutax.tungsten.commands;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import kaptainwutax.tungsten.TungstenMod;
import kaptainwutax.tungsten.commandsystem.Command;
import kaptainwutax.tungsten.commandsystem.CommandException;
import kaptainwutax.tungsten.task.FollowPlayerTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;
import java.util.concurrent.CompletableFuture;

public class FollowPlayerCommand extends Command {

    public FollowPlayerCommand(TungstenMod mod) throws CommandException {
        super("followPlayer", "Follow a player by name (re-discovers if they disappear)", mod);
    }

    @Override
    public void build(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        // ;followPlayer <name>  — Tab shows online players from the server tab list
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

        // ;followPlayer <name>            — push mode (default, no stopping)
        // ;followPlayer <name> <radius>   — follow at radius distance
        builder.then(argument("name", StringArgumentType.word())
                .suggests(playerSuggestions)
                .executes(context -> {
                    String name = StringArgumentType.getString(context, "name");
                    FollowPlayerTask.start(name);
                    return SINGLE_SUCCESS;
                })
                .then(argument("radius", DoubleArgumentType.doubleArg(0.0, 64.0))
                        .executes(context -> {
                            String name   = StringArgumentType.getString(context, "name");
                            double radius = DoubleArgumentType.getDouble(context, "radius");
                            FollowPlayerTask.start(name, radius);
                            return SINGLE_SUCCESS;
                        })));
    }
}

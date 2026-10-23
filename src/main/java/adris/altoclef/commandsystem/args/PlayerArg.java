package adris.altoclef.commandsystem.args;

import adris.altoclef.commandsystem.StringReader;
import adris.altoclef.commandsystem.exception.CommandNotFinishedException;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

/**
 * String argument with tab-completion from the server player list.
 * Drop-in replacement for StringArg("playerName") in any command.
 */
public class PlayerArg extends Arg<String> {

    public PlayerArg(String name) {
        super(name);
    }

    public PlayerArg(String name, String defaultValue) {
        super(name, defaultValue);
    }

    public PlayerArg(String name, String defaultValue, boolean showDefault) {
        super(name, defaultValue, showDefault);
    }

    public static Stream<String> listOnlinePlayers() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return Stream.empty();
        return mc.getConnection().getOnlinePlayers().stream()
                .map(PlayerInfo::getProfile)
                .map(p -> p.name())
                .filter(name -> !name.isEmpty());
    }

    @Override
    public Stream<String> getSuggestions(StringReader reader) {
        return listOnlinePlayers();
    }

    @Override
    protected StringParser<String> getParser() {
        return reader -> {
            String value = reader.next();
            if (value.isEmpty()) throw new CommandNotFinishedException("Player name cannot be empty");
            return value;
        };
    }

    @Override
    public String getTypeName() {
        return "Player";
    }

    @Override
    public Class<String> getType() {
        return String.class;
    }
}
